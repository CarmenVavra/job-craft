package com.jobcraft.service.ai;

import com.jobcraft.model.LetterLength;
import com.jobcraft.model.Requirement;
import java.util.Optional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regel- und vorlagenbasierte Umsetzung ohne Sprachmodell (Demo-Modus, wenn kein API-Key konfiguriert ist).
 * Extraktion per Heuristiken, Texte aus Bausteinen, Chat über Stichwortregeln.
 */
public class TemplateCoverLetterAi implements CoverLetterAi {

    private static final Pattern CONTACT = Pattern.compile(
            "\\b(Frau|Herr)\\s+((?:(?:Dr|Prof)\\.\\s+)*[A-ZÄÖÜ][\\wäöüß-]+(?:\\s+[A-ZÄÖÜ][\\wäöüß-]+)?)");
    private static final Pattern ADDRESS = Pattern.compile(
            "\\b([A-ZÄÖÜ][\\wäöüß.\\-]*(?:\\s[\\wäöüß.\\-]+)?\\s\\d{1,4}[a-z]?)[,\\s]+(\\d{5}\\s[A-ZÄÖÜ][\\wäöüß\\-]+)");
    private static final Pattern COMPANY = Pattern.compile(
            "\\b([A-ZÄÖÜ][\\wäöüß&.\\-]*(?:[ \\t][A-ZÄÖÜ&][\\wäöüß&.\\-]*){0,3}[ \\t]"
                    + "(?:GmbH(?: & Co\\. KG)?|AG|SE|KG|mbH|e\\.V\\.))");
    private static final Pattern TASK_HEADING = Pattern.compile(
            "(?i)(aufgaben|tätigkeit|das erwartet dich|was dich erwartet|deine rolle|verantwortung)");
    private static final Pattern REQ_HEADING = Pattern.compile(
            "(?i)(profil|anforderung|bringst du mit|qualifikation|was du mitbringst|voraussetzung|wir erwarten|skills)");
    private static final Pattern TITLE_SUFFIX = Pattern.compile("\\s+[–|\\-]\\s+.*$");
    private static final Pattern ENGLISH_LEVEL = Pattern.compile("(?i)englisch\\W{0,3}\\(?\\s*([abc][12])");
    private static final Set<String> STOPWORDS = Set.of("und", "oder", "mit", "der", "die", "das", "für", "von",
            "sehr", "gute", "guter", "gutes", "kenntnisse", "erfahrung", "erfahrungen", "fließend", "mehrjährige",
            "idealerweise", "sicherer", "umgang", "sowie", "erste", "wünschenswert", "bereitschaft");
    private static final int MAX_REQUIREMENTS = 8;
    private static final int MAX_TASKS = 6;
    private static final int STANDARD_PARAGRAPHS = 5;
    /** Anteil der Stichworte einer Anforderung, die im Profil vorkommen müssen. */
    private static final double MATCH_RATIO = 0.6;

    @Override
    public boolean isLanguageModel() {
        return false;
    }

    @Override
    public JobExtraction extractJob(String jobText, String pageTitle, String siteName) {
        String title = pageTitle == null ? "" : pageTitle.trim();
        if (title.length() > 0 && siteName != null && !siteName.isBlank()) {
            title = TITLE_SUFFIX.matcher(title).replaceFirst("");
        }
        String company = siteName != null && !siteName.isBlank() ? siteName.trim() : find(COMPANY, jobText, 1);
        Matcher contact = CONTACT.matcher(jobText);
        String contactPerson = contact.find() ? contact.group(1) + " " + contact.group(2) : "";
        Matcher address = ADDRESS.matcher(jobText);
        String recipientAddress = address.find() ? address.group(1).trim() + ", " + address.group(2).trim() : "";

        List<String> tasks = new ArrayList<>();
        List<String> requirements = new ArrayList<>();
        List<String> current = null;
        for (String rawLine : jobText.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("- ")) {
                if (current != null) {
                    current.add(line.substring(2).trim());
                }
            } else if (line.length() < 60 && TASK_HEADING.matcher(line).find()) {
                current = tasks;
            } else if (line.length() < 60 && REQ_HEADING.matcher(line).find()) {
                current = requirements;
            } else if (line.length() < 60) {
                current = null;
            }
        }
        return new JobExtraction(company, title, contactPerson, recipientAddress,
                limit(requirements, MAX_REQUIREMENTS), limit(tasks, MAX_TASKS));
    }

    @Override
    public List<Boolean> matchRequirements(List<String> requirements, String applicantText) {
        String haystack = applicantText == null ? "" : applicantText.toLowerCase(Locale.GERMAN);
        return requirements.stream().map(r -> {
            Set<String> words = keywords(r);
            if (haystack.isBlank() || words.isEmpty()) {
                return false;
            }
            long hits = words.stream()
                    .filter(k -> haystack.contains(k.substring(0, Math.min(k.length(), Math.max(4, k.length() - 2)))))
                    .count();
            return hits >= Math.ceil(words.size() * MATCH_RATIO);
        }).toList();
    }

    @Override
    public List<String> generate(GenerationContext ctx) {
        List<String> matched = ctx.requirements().stream().filter(Requirement::matched).map(Requirement::text).toList();
        List<String> unmatched = ctx.requirements().stream().filter(r -> !r.matched()).map(Requirement::text).toList();
        String intro = intro(ctx);
        String skills = matched.isEmpty()
                ? "Meine bisherigen beruflichen Stationen haben mich gut auf diese Aufgaben vorbereitet – die Details "
                + "finden Sie in meinem Lebenslauf."
                : "Fachlich bringe ich genau die Schwerpunkte mit, die Sie suchen: " + joinGerman(matched)
                + ". Diese Kenntnisse habe ich in meiner bisherigen Praxis eingesetzt und kontinuierlich vertieft.";
        String tasks = tasksParagraph(ctx.tasks());
        String soft = switch (ctx.tone()) {
            case CASUAL -> "Ich arbeite am liebsten im Team, packe mit an und sage offen, was ich denke – "
                    + "immer mit Blick auf ein gutes Ergebnis für alle.";
            case MODERN -> "Ich arbeite gern in interdisziplinären Teams, übernehme Verantwortung und schätze "
                    + "kurze Wege, klares Feedback und gemeinsames Lernen.";
            case FORMAL -> "Ich arbeite gern in agilen, interdisziplinären Teams, übernehme Verantwortung und lege "
                    + "Wert auf eine klare, wertschätzende Kommunikation.";
        };
        String closing = closing(ctx);

        List<String> paragraphs = new ArrayList<>();
        if (ctx.length() == LetterLength.COMPACT) {
            paragraphs.add(intro);
            paragraphs.add(skills + " " + soft);
            paragraphs.add(closing);
            return paragraphs;
        }
        paragraphs.add(intro);
        paragraphs.add(skills);
        paragraphs.add(tasks);
        if (ctx.length() == LetterLength.DETAILED) {
            paragraphs.add(unmatched.isEmpty()
                    ? "Was mich an Ihrem Unternehmen besonders überzeugt, ist der Anspruch, Produkte konsequent aus "
                    + "Sicht der Nutzerinnen und Nutzer zu denken – genau so verstehe ich gute Softwareentwicklung."
                    : gapSentence(unmatched) + " Neue Themen eigne ich mir erfahrungsgemäß schnell und strukturiert an.");
        }
        paragraphs.add(soft);
        paragraphs.add(closing);
        return paragraphs;
    }

    @Override
    public LetterRevision revise(GenerationContext ctx, List<String> current, String instruction) {
        String low = instruction.toLowerCase(Locale.GERMAN);
        List<String> next = new ArrayList<>(current);
        int last = next.size() - 1;
        List<String> unmatched = ctx.requirements().stream().filter(r -> !r.matched()).map(Requirement::text).toList();
        List<String> matched = ctx.requirements().stream().filter(Requirement::matched).map(Requirement::text).toList();
        int techIndex = Math.min(1, last);
        int taskIndex = Math.min(2, last);
        int softIndex = next.size() >= STANDARD_PARAGRAPHS ? last - 1 : Math.min(1, last);

        if (containsAny(low, "kürz", "kurz", "knapp", "einleitung")) {
            next.set(0, firstSentence(next.get(0)));
            return result("Die Einleitung ist jetzt auf einen Satz gekürzt.", next, 0);
        }
        if (containsAny(low, "prägnant", "präzise", "java", "backend", "fachlich", "technisch")) {
            next.set(techIndex, matched.isEmpty()
                    ? "Kurz gesagt: Ich bringe solide Praxis in den Kernthemen der Stelle mit und arbeite mich schnell "
                    + "in neue Technologien ein."
                    : "Kurz gesagt: " + joinGerman(matched) + " gehören zu meinem täglichen Handwerkszeug – "
                    + "praxiserprobt und auf aktuellem Stand.");
            return result("Ich habe den fachlichen Absatz gestrafft und die Kernkompetenzen nach vorne gezogen.",
                    next, techIndex);
        }
        if (containsAny(low, "soft", "team", "projektmanagement", "führung", "kommunikation")) {
            next.set(softIndex, "Neben der Technik liegt mir die Koordination: Ich strukturiere Aufgaben, stimme "
                    + "mich eng mit Stakeholdern ab und behalte Termine und Qualität gleichermaßen im Blick.");
            return result("Die Soft Skills sind jetzt konkreter: Koordination, Stakeholder-Abstimmung, Priorisierung.",
                    next, softIndex);
        }
        if (containsAny(low, "englisch", "sprache", "international")) {
            String level = documentsText(ctx).map(t -> {
                Matcher m = ENGLISH_LEVEL.matcher(t);
                return m.find() ? " (" + m.group(1).toUpperCase(Locale.ROOT) + ")" : "";
            }).orElse("");
            next.set(taskIndex, next.get(taskIndex) + " Englisch" + level
                    + " nutze ich sicher in Wort und Schrift – auch im täglichen Austausch mit internationalen Teams.");
            return result("Ich habe deine Englischkenntnisse ergänzt.", next, taskIndex);
        }
        if (containsAny(low, "lücke", "fehlt", "fehlend", "kubernetes", "docker", "lernbereit") && !unmatched.isEmpty()) {
            next.set(taskIndex, next.get(taskIndex) + " " + gapSentence(unmatched));
            return result("Die Lücke im Abgleich habe ich ehrlich als ausbaufähige Erfahrung formuliert.", next,
                    taskIndex);
        }
        next.set(last, "Ich freue mich darauf, Sie in einem persönlichen Gespräch kennenzulernen und mehr über "
                + "Ihr Team zu erfahren." + availability(ctx));
        return result("Ich habe den Schlussabsatz angepasst. Hinweis: Im Demo-Modus ohne KI-Schlüssel setze ich nur "
                + "typische Änderungswünsche um.", next, last);
    }

    private static String intro(GenerationContext ctx) {
        String role = cleanRole(ctx.jobTitle());
        String company = companyWithArticle(ctx.companyName());
        String first = switch (ctx.tone()) {
            case CASUAL -> "als ich Ihre Anzeige für die Position als " + role + " " + company
                    + " gesehen habe, war mir schnell klar: Das passt.";
            case MODERN -> "Ihre Ausschreibung als " + role + " " + company + " hat sofort mein Interesse geweckt.";
            case FORMAL -> "mit großem Interesse habe ich Ihre Ausschreibung für die Position als " + role + " "
                    + company + " gelesen.";
        };
        if (ctx.tasks().isEmpty()) {
            return first;
        }
        return first + " Besonders spricht mich der Aufgabenbereich „" + ctx.tasks().get(0) + "“ an.";
    }

    private static String tasksParagraph(List<String> tasks) {
        if (tasks.size() >= 3) {
            return "Die von Ihnen beschriebenen Aufgaben – insbesondere „" + tasks.get(1) + "“ und „" + tasks.get(2)
                    + "“ – entsprechen genau dem, was ich in meiner nächsten Position einbringen und weiterentwickeln "
                    + "möchte.";
        }
        return "Die von Ihnen beschriebenen Aufgaben entsprechen genau dem, was ich in meiner nächsten Position "
                + "einbringen und weiterentwickeln möchte.";
    }

    private static String closing(GenerationContext ctx) {
        return "Über die Gelegenheit, Sie in einem persönlichen Gespräch von mir zu überzeugen, freue ich mich sehr."
                + availability(ctx);
    }

    private static String availability(GenerationContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (String field : ctx.customFields()) {
            String lower = field.toLowerCase(Locale.GERMAN);
            int colon = field.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String value = field.substring(colon + 1).trim();
            if (lower.startsWith("verfügbar") || lower.startsWith("eintritt")) {
                sb.append(" Meine frühestmögliche Verfügbarkeit ist ").append(value).append('.');
            } else if (lower.startsWith("gehalt")) {
                sb.append(" Meine Gehaltsvorstellung liegt bei ").append(value).append('.');
            }
        }
        return sb.toString();
    }

    private static String gapSentence(List<String> unmatched) {
        return "Im Bereich " + joinGerman(unmatched) + " habe ich erste Erfahrungen gesammelt und baue sie aktuell "
                + "gezielt aus.";
    }

    private static Optional<String> documentsText(GenerationContext ctx) {
        String text = String.join("\n", ctx.documents().stream().map(GenerationContext.DocumentText::text).toList());
        return text.isBlank() ? Optional.empty() : Optional.of(text);
    }

    private static LetterRevision result(String reply, List<String> paragraphs, int changed) {
        return new LetterRevision(reply, paragraphs, List.of(changed));
    }

    /**
     * Entfernt Gender-Zusätze wie „(m/w/d)“ aus der Positionsbezeichnung.
     *
     * @param role Positionsbezeichnung
     * @return bereinigte Bezeichnung
     */
    public static String cleanRole(String role) {
        return role == null ? "" : role.replaceAll("(?i)\\s*\\((?:m|w|d|f|x|i|div)(?:\\s*/\\s*(?:m|w|d|f|x|i|div))+\\)", "")
                .replaceAll("(?i)\\s*\\(all genders?\\)", "").trim();
    }

    private static String companyWithArticle(String company) {
        String name = company == null || company.isBlank() ? "Ihrem Unternehmen" : company.trim();
        if (name.matches(".*\\b(GmbH|AG|SE|KG|mbH)(\\s*&\\s*Co\\.\\s*KG)?$")) {
            return "bei der " + name;
        }
        return "bei " + name;
    }

    private static String firstSentence(String paragraph) {
        Matcher m = Pattern.compile("^(.+?[.!?])(\\s|$)").matcher(paragraph);
        return m.find() ? m.group(1) : paragraph;
    }

    private static Set<String> keywords(String requirement) {
        Set<String> words = new LinkedHashSet<>();
        for (String word : requirement.toLowerCase(Locale.GERMAN).split("[^\\p{L}\\d#+]+")) {
            if (word.length() >= 3 && !STOPWORDS.contains(word) && !word.chars().allMatch(Character::isDigit)) {
                words.add(word);
            }
        }
        return words;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String find(Pattern pattern, String text, int group) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(group).trim() : "";
    }

    private static List<String> limit(List<String> items, int max) {
        return items.stream().filter(s -> !s.isBlank()).distinct().limit(max).toList();
    }

    /**
     * Verbindet Elemente zu „a, b und c“.
     *
     * @param items Elemente
     * @return deutsche Aufzählung
     */
    static String joinGerman(List<String> items) {
        if (items.size() <= 1) {
            return items.isEmpty() ? "" : items.get(0);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " und " + items.get(items.size() - 1);
    }
}
