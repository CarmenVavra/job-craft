import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { testProviders, text } from '../../../testing/providers';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [LoginComponent], providers: testProviders() });
  });

  async function setup() {
    const fixture = TestBed.createComponent(LoginComponent);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  function type(el: HTMLElement, selector: string, value: string): void {
    const input = el.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  it('validates the email before calling the API', async () => {
    const { fixture, el } = await setup();
    type(el, 'input[name=email]', 'keine-mail');
    type(el, 'input[name=password]', 'geheim123');
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(text(el.querySelector('[role=alert]'))).toBe(
      'Bitte gib eine gültige E-Mail-Adresse ein.',
    );
    TestBed.inject(HttpTestingController).expectNone('/api/auth/login');
  });

  it('requires a name when registering', async () => {
    const { fixture, el } = await setup();
    (el.querySelectorAll('.tabs button')[1] as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(text(el.querySelector('h2'))).toBe('Konto erstellen');

    type(el, 'input[name=email]', 'lena@example.de');
    type(el, 'input[name=password]', 'geheim123');
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    expect(text(el.querySelector('[role=alert]'))).toBe('Bitte gib deinen Namen ein.');
  });

  it('logs in and navigates to the dashboard', async () => {
    const { fixture, el } = await setup();
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    type(el, 'input[name=email]', 'lena@example.de');
    type(el, 'input[name=password]', 'geheim123');
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    TestBed.inject(HttpTestingController)
      .expectOne('/api/auth/login')
      .flush({ token: 'a.b.c', user: { id: 'u1', email: 'lena@example.de', name: 'Lena' } });
    await fixture.whenStable();
    expect(navigate).toHaveBeenCalledWith(['/uebersicht']);
  });

  it('shows the server error message', async () => {
    const { fixture, el } = await setup();
    type(el, 'input[name=email]', 'lena@example.de');
    type(el, 'input[name=password]', 'falsch123');
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    TestBed.inject(HttpTestingController)
      .expectOne('/api/auth/login')
      .flush({ message: 'E-Mail oder Passwort ist falsch.' }, { status: 401, statusText: 'x' });
    await fixture.whenStable();
    expect(text(el.querySelector('[role=alert]'))).toBe('E-Mail oder Passwort ist falsch.');
  });
});
