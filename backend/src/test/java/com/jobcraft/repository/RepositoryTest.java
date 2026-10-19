package com.jobcraft.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobcraft.model.Application;
import com.jobcraft.model.ApplicationStatus;
import com.jobcraft.model.CoverLetter;
import com.jobcraft.model.CustomField;
import com.jobcraft.model.Profile;
import com.jobcraft.model.Requirement;
import com.jobcraft.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class RepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CoverLetterRepository coverLetterRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("lena@example.de");
        user.setPasswordHash("{noop}x");
        userId = userRepository.save(user).getId();
    }

    @Test
    void findsUserByEmailIgnoringCase() {
        assertThat(userRepository.findByEmailIgnoreCase("LENA@example.de")).isPresent();
        assertThat(userRepository.existsByEmailIgnoreCase("other@example.de")).isFalse();
    }

    @Test
    void storesCustomFieldsAsJson() {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setFullName("Lena Hoffmann");
        profile.setCustomFields(new ArrayList<>(List.of(new CustomField("Führerschein", "Klasse B"))));
        profileRepository.save(profile);
        em.flush();
        em.clear();

        Profile loaded = profileRepository.findByUserId(userId).orElseThrow();
        assertThat(loaded.getCustomFields()).containsExactly(new CustomField("Führerschein", "Klasse B"));
    }

    @Test
    void loadsLatestVersionsInOneQuery() {
        Application first = application(ApplicationStatus.DRAFT, List.of(new Requirement("Java", true)));
        Application second = application(ApplicationStatus.SENT, List.of());
        application(ApplicationStatus.ANALYZING, List.of());
        version(first.getId(), 1);
        version(first.getId(), 2);
        version(second.getId(), 1);
        em.flush();
        em.clear();

        Map<UUID, Integer> latest = coverLetterRepository.findLatestVersions(List.of(first.getId(), second.getId()))
                .stream().collect(Collectors.toMap(LatestVersion::applicationId, LatestVersion::latestVersion));
        List<Application> listed = applicationRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(userId,
                List.of(ApplicationStatus.DRAFT, ApplicationStatus.SENT));

        assertThat(latest).containsEntry(first.getId(), 2).containsEntry(second.getId(), 1);
        assertThat(listed).hasSize(2);
        assertThat(applicationRepository.findById(first.getId()).orElseThrow().getRequirements())
                .containsExactly(new Requirement("Java", true));
    }

    @Test
    void scopesApplicationsToOwner() {
        Application app = application(ApplicationStatus.DRAFT, List.of());

        assertThat(applicationRepository.findByIdAndUserId(app.getId(), userId)).isPresent();
        assertThat(applicationRepository.findByIdAndUserId(app.getId(), UUID.randomUUID())).isEmpty();
    }

    private Application application(ApplicationStatus status, List<Requirement> requirements) {
        Application app = new Application();
        app.setUserId(userId);
        app.setJobUrl("https://example.de/job");
        app.setStatus(status);
        app.setRequirements(new ArrayList<>(requirements));
        return applicationRepository.save(app);
    }

    private void version(UUID applicationId, int version) {
        CoverLetter letter = new CoverLetter();
        letter.setApplicationId(applicationId);
        letter.setVersion(version);
        letter.setParagraphs(new ArrayList<>(List.of("Absatz")));
        coverLetterRepository.save(letter);
    }
}
