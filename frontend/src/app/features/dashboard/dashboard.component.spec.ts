import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { profileFixture, summaryFixture } from '../../../testing/fixtures';
import { testProviders, text } from '../../../testing/providers';
import { ProfileStore } from '../../core/api/profile.store';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [DashboardComponent], providers: testProviders() });
    http = TestBed.inject(HttpTestingController);
  });

  async function setup() {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    TestBed.inject(ProfileStore).load().subscribe();
    http.expectOne('/api/profile').flush(profileFixture());
    http
      .expectOne('/api/applications')
      .flush([
        summaryFixture(),
        summaryFixture({ id: 'app-2', status: 'SENT', companyName: 'Helio Energy AG' }),
      ]);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('greets the user and lists applications', async () => {
    const { el } = await setup();
    expect(text(el.querySelector('h1'))).toBe('Guten Tag, Lena');
    expect(text(el.querySelector('.jc-page-head p'))).toContain('1 Anschreiben im Entwurf');
    expect(el.querySelectorAll('.row:not(.header)').length).toBe(2);
    expect(text(el.querySelector('.status-value'))).toBe('100 %');
  });

  it('validates the URL before starting an analysis', async () => {
    const { fixture, el } = await setup();
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    (el.querySelector('.new-card') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    expect(text(el.querySelector('.url-error'))).toContain('gültigen Link');

    (el.querySelector('.sample') as HTMLButtonElement).click();
    (el.querySelector('.new-card') as HTMLFormElement).dispatchEvent(new Event('submit'));
    expect(navigate).toHaveBeenCalledWith(['/bewerbung/neu'], {
      queryParams: { url: 'https://karriere.quantis-software.de/jobs/senior-fullstack-entwickler' },
    });
  });

  it('changes the status of an application', async () => {
    const { fixture, el } = await setup();
    (el.querySelector('.status-pill') as HTMLButtonElement).click();
    await fixture.whenStable();
    const items = el.querySelectorAll('.status-menu button');
    expect(items.length).toBe(4);
    (items[1] as HTMLButtonElement).click();

    const req = http.expectOne('/api/applications/app-1/status');
    expect(req.request.body).toEqual({ status: 'SENT' });
    req.flush({});
    await fixture.whenStable();
    expect(text(el.querySelector('.status-pill'))).toBe('Versendet');
  });
});
