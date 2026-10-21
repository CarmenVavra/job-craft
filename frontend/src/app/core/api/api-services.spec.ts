import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  applicationFixture,
  documentFixture,
  noteFixture,
  profileFixture,
} from '../../../testing/fixtures';
import { ApplicationDetail } from '../models/api.models';
import { ApplicationService } from './application.service';
import { DocumentStore } from './document.store';
import { NoteStore } from './note.store';
import { ProfileStore } from './profile.store';
import { SystemService } from './system.service';

describe('API services', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('ProfileStore', () => {
    it('loads and saves the profile into its signal', () => {
      const store = TestBed.inject(ProfileStore);
      store.load().subscribe();
      http.expectOne('/api/profile').flush(profileFixture());
      expect(store.profile()?.fullName).toBe('Lena Hoffmann');

      store.save(profileFixture({ fullName: 'Lena H.' })).subscribe();
      const req = http.expectOne('/api/profile');
      expect(req.request.method).toBe('PUT');
      req.flush(profileFixture({ fullName: 'Lena H.' }));
      expect(store.profile()?.fullName).toBe('Lena H.');
    });
  });

  describe('NoteStore', () => {
    it('loads, creates, updates and removes notes', () => {
      const store = TestBed.inject(NoteStore);
      store.load().subscribe();
      http.expectOne('/api/notes').flush([noteFixture()]);
      expect(store.includedNotes().length).toBe(1);

      store.create({ title: 'Ehrenamt', content: 'Trainerin', included: true }).subscribe();
      const create = http.expectOne('/api/notes');
      expect(create.request.method).toBe('POST');
      create.flush(noteFixture({ id: 'note-2', title: 'Ehrenamt' }));
      expect(store.notes().map((n) => n.title)).toEqual([
        'Privatprojekt: Haushaltsbuch-App',
        'Ehrenamt',
      ]);

      store.update('note-1', { title: 'Privatprojekt', content: 'x', included: false }).subscribe();
      const update = http.expectOne('/api/notes/note-1');
      expect(update.request.method).toBe('PUT');
      update.flush(noteFixture({ included: false }));
      expect(store.includedNotes().map((n) => n.id)).toEqual(['note-2']);

      store.remove('note-2').subscribe();
      http.expectOne({ method: 'DELETE', url: '/api/notes/note-2' }).flush(null);
      expect(store.notes().length).toBe(1);
    });
  });

  describe('DocumentStore', () => {
    beforeEach(() => jasmine.clock().install());
    afterEach(() => jasmine.clock().uninstall());

    it('polls while documents are processing', () => {
      const store = TestBed.inject(DocumentStore);
      store.load().subscribe();
      http.expectOne('/api/documents').flush([documentFixture({ status: 'PROCESSING' })]);
      expect(store.processing()).toBeTrue();

      jasmine.clock().tick(1500);
      http.expectOne('/api/documents').flush([documentFixture({ status: 'READY' })]);
      expect(store.readyDocuments().length).toBe(1);

      jasmine.clock().tick(3000);
      http.expectNone('/api/documents');
    });

    it('uploads, updates and removes documents', () => {
      const store = TestBed.inject(DocumentStore);
      store.upload(new File(['%PDF'], 'cv.pdf')).subscribe();
      const upload = http.expectOne('/api/documents');
      expect(upload.request.body instanceof FormData).toBeTrue();
      upload.flush(documentFixture());

      store.updateCategory('doc-1', 'SKILLS').subscribe();
      http.expectOne('/api/documents/doc-1').flush(documentFixture({ category: 'SKILLS' }));
      expect(store.documents()[0].category).toBe('SKILLS');

      store.remove('doc-1').subscribe();
      http.expectOne('/api/documents/doc-1').flush(null);
      expect(store.documents().length).toBe(0);
    });
  });

  describe('ApplicationService', () => {
    it('polls the analysis until it is finished', () => {
      jasmine.clock().install();
      const service = TestBed.inject(ApplicationService);
      const states: ApplicationDetail[] = [];
      service.watchAnalysis('app-1').subscribe((app) => states.push(app));

      jasmine.clock().tick(1);
      http
        .expectOne('/api/applications/app-1')
        .flush(applicationFixture({ status: 'ANALYZING', analysisStep: 1 }));
      jasmine.clock().tick(500);
      http.expectOne('/api/applications/app-1').flush(applicationFixture({ status: 'ANALYZED' }));
      jasmine.clock().tick(1000);
      jasmine.clock().uninstall();

      expect(states.map((s) => s.status)).toEqual(['ANALYZING', 'ANALYZED']);
    });

    it('requests the PDF with all export options', () => {
      const service = TestBed.inject(ApplicationService);
      service
        .pdf('app-1', { version: 2, template: 'MODERN', attachCv: true, attachCertificates: false })
        .subscribe();
      const req = http.expectOne((r) => r.url === '/api/applications/app-1/pdf');
      expect(req.request.params.get('template')).toBe('MODERN');
      expect(req.request.params.get('attachCv')).toBe('true');
      expect(req.request.responseType).toBe('blob');
      req.flush(new Blob(['%PDF']));
    });

    it('posts chat messages', () => {
      TestBed.inject(ApplicationService).chat('app-1', 'Einleitung kürzen').subscribe();
      const req = http.expectOne('/api/applications/app-1/chat');
      expect(req.request.body).toEqual({ message: 'Einleitung kürzen' });
      req.flush(applicationFixture());
    });
  });

  describe('SystemService', () => {
    it('loads the system info once', () => {
      const service = TestBed.inject(SystemService);
      service.load();
      service.load();
      http.expectOne('/api/system').flush({ aiEnabled: false });
      expect(service.info()?.aiEnabled).toBeFalse();
    });
  });
});
