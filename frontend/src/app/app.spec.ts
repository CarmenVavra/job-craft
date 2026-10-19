import { TestBed } from '@angular/core/testing';
import { testProviders } from '../testing/providers';
import { App } from './app';

describe('App', () => {
  it('creates the root component with a router outlet', async () => {
    TestBed.configureTestingModule({ imports: [App], providers: testProviders() });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('router-outlet')).not.toBeNull();
  });
});
