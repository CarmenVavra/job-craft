import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { profileFixture } from '../../../testing/fixtures';
import { testProviders, text } from '../../../testing/providers';
import { ProfileStore } from '../../core/api/profile.store';
import { ToastService } from '../../core/ui/toast.service';
import { ProfileComponent, splitZipCity } from './profile.component';

describe('ProfileComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ProfileComponent], providers: testProviders() });
    http = TestBed.inject(HttpTestingController);
  });

  async function setup() {
    TestBed.inject(ProfileStore).load().subscribe();
    http.expectOne('/api/profile').flush(profileFixture());
    const fixture = TestBed.createComponent(ProfileComponent);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  function type(el: HTMLElement, selector: string, value: string): void {
    const input = el.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  it('fills the form and the live preview from the profile', async () => {
    const { el } = await setup();
    expect((el.querySelector('input[name=zipCity]') as HTMLInputElement).value).toBe('50674 Köln');
    expect(text(el.querySelector('app-letter-sheet .name'))).toBe('Lena Hoffmann');
    expect(el.querySelectorAll('.custom-row').length).toBe(1);
  });

  it('saves the letterhead with split zip code and city', async () => {
    const { fixture, el } = await setup();
    type(el, 'input[name=zipCity]', '10115 Berlin');
    (el.querySelectorAll('.style-tabs button')[1] as HTMLButtonElement).click();
    (el.querySelector('.add') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(el.querySelectorAll('.custom-row').length).toBe(2);

    (el.querySelector('.jc-page-head .jc-btn') as HTMLButtonElement).click();
    const req = http.expectOne('/api/profile');
    expect(req.request.body.zipCode).toBe('10115');
    expect(req.request.body.city).toBe('Berlin');
    expect(req.request.body.letterStyle).toBe('MODERN');
    req.flush(req.request.body);
    expect(TestBed.inject(ToastService).message()).toBe('Briefkopf gespeichert');
  });

  it('validates and changes the password', async () => {
    const { fixture, el } = await setup();
    const form = el.querySelector('.password-form') as HTMLFormElement;
    type(el, 'input[name=currentPassword]', 'geheim123');
    type(el, 'input[name=newPassword]', 'neuesPasswort1');
    type(el, 'input[name=confirmPassword]', 'anderesPasswort');
    form.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    expect(text(el.querySelector('.password-form [role=alert]'))).toContain(
      'stimmen nicht überein',
    );

    type(el, 'input[name=confirmPassword]', 'neuesPasswort1');
    form.dispatchEvent(new Event('submit'));
    const req = http.expectOne('/api/auth/password');
    expect(req.request.body).toEqual({
      currentPassword: 'geheim123',
      newPassword: 'neuesPasswort1',
    });
    req.flush(null);
    expect(TestBed.inject(ToastService).message()).toBe('Passwort geändert');
  });

  it('splits zip code and city', () => {
    expect(splitZipCity('50674 Köln')).toEqual({ zipCode: '50674', city: 'Köln' });
    expect(splitZipCity('Köln')).toEqual({ zipCode: null, city: 'Köln' });
    expect(splitZipCity('')).toEqual({ zipCode: null, city: null });
  });
});
