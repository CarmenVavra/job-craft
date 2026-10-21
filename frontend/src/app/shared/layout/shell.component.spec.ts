import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { documentFixture, noteFixture, profileFixture } from '../../../testing/fixtures';
import { testProviders, text } from '../../../testing/providers';
import { AuthService } from '../../core/auth/auth.service';
import { ShellComponent } from './shell.component';

describe('ShellComponent', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: testProviders() }));

  it('loads profile, documents, notes and system info and shows the user', async () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/profile').flush(profileFixture());
    http.expectOne('/api/documents').flush([documentFixture()]);
    http.expectOne('/api/notes').flush([noteFixture()]);
    http.expectOne('/api/system').flush({ aiEnabled: true });
    await fixture.whenStable();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.nav-item').length).toBe(5);
    expect(text(el.querySelector('.nav-item.active'))).toBe('Übersicht');
    expect(text(el.querySelector('.avatar'))).toBe('LH');
    expect(text(el.querySelector('.name'))).toBe('Lena Hoffmann');
  });

  it('logs out', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    const logout = spyOn(TestBed.inject(AuthService), 'logout');
    (fixture.nativeElement.querySelector('.logout') as HTMLButtonElement).click();
    expect(logout).toHaveBeenCalled();
  });
});
