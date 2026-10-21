import {
  ApplicationDetail,
  ApplicationSummary,
  DocumentSummary,
  Profile,
  ProfileNote,
} from '../app/core/models/api.models';

/** Testdaten, angelehnt an die Mock-Daten des Prototyps. */
export function profileFixture(overrides: Partial<Profile> = {}): Profile {
  return {
    fullName: 'Lena Hoffmann',
    street: 'Lindenstraße 12',
    zipCode: '50674',
    city: 'Köln',
    phone: '+49 171 2345678',
    email: 'lena.hoffmann@mail.de',
    linkedin: 'linkedin.com/in/lenahoffmann',
    github: 'github.com/lhoffmann',
    letterStyle: 'CLASSIC',
    customFields: [{ label: 'Führerschein', value: 'Klasse B' }],
    ...overrides,
  };
}

export function documentFixture(overrides: Partial<DocumentSummary> = {}): DocumentSummary {
  return {
    id: 'doc-1',
    fileName: 'Lebenslauf_Lena_Hoffmann_2026.pdf',
    contentType: 'application/pdf',
    category: 'CV',
    sizeBytes: 253952,
    pageCount: 2,
    status: 'READY',
    createdAt: '2026-10-02T10:00:00Z',
    ...overrides,
  };
}

export function noteFixture(overrides: Partial<ProfileNote> = {}): ProfileNote {
  return {
    id: 'note-1',
    title: 'Privatprojekt: Haushaltsbuch-App',
    content: 'Angular 21 mit Signals, Spring Boot und PostgreSQL',
    included: true,
    createdAt: '2026-10-20T10:00:00Z',
    updatedAt: '2026-10-21T10:00:00Z',
    ...overrides,
  };
}

export function summaryFixture(overrides: Partial<ApplicationSummary> = {}): ApplicationSummary {
  return {
    id: 'app-1',
    companyName: 'Nordwind Logistik GmbH',
    jobTitle: 'Senior Java Developer',
    status: 'DRAFT',
    createdAt: '2026-10-14T10:00:00Z',
    latestVersion: 3,
    ...overrides,
  };
}

export function applicationFixture(overrides: Partial<ApplicationDetail> = {}): ApplicationDetail {
  return {
    id: 'app-1',
    jobUrl: 'https://karriere.quantis-software.de/jobs/senior-fullstack-entwickler',
    jobTitle: 'Senior Fullstack Entwickler (m/w/d)',
    companyName: 'Quantis Software GmbH',
    contactPerson: 'Frau Dr. Miriam Kessler',
    recipientAddress: 'Am Mediapark 8, 50670 Köln',
    customSalutation: null,
    requirements: [
      { text: 'Java 17+ & Spring Boot', matched: true },
      { text: 'Kubernetes / Docker', matched: false },
    ],
    tasks: ['Konzeption neuer Features'],
    contextDocumentIds: ['doc-1'],
    tone: 'FORMAL',
    length: 'STANDARD',
    status: 'DRAFT',
    analysisStep: 4,
    errorMessage: null,
    createdAt: '2026-10-19T10:00:00Z',
    letter: {
      senderName: 'Lena Hoffmann',
      senderLine: 'Lena Hoffmann · Lindenstraße 12 · 50674 Köln',
      recipientLines: [
        'Quantis Software GmbH',
        'Frau Dr. Miriam Kessler',
        'Am Mediapark 8',
        '50670 Köln',
      ],
      dateLine: 'Köln, 19. Oktober 2026',
      subject: 'Bewerbung als Senior Fullstack Entwickler',
      salutation: 'Sehr geehrte Frau Dr. Kessler,',
      closing: 'Mit freundlichen Grüßen',
    },
    versions: [
      {
        version: 1,
        paragraphs: ['Absatz eins', 'Absatz zwei'],
        changedParagraphs: [],
        createdAt: '',
      },
      {
        version: 2,
        paragraphs: ['Absatz eins', 'Absatz zwei neu'],
        changedParagraphs: [1],
        createdAt: '',
      },
    ],
    messages: [
      {
        id: 'm1',
        role: 'ASSISTANT',
        message: 'Dein erster Entwurf ist fertig.',
        meta: 'v1 erstellt',
        coverLetterVersion: 1,
        createdAt: '',
      },
    ],
    ...overrides,
  };
}
