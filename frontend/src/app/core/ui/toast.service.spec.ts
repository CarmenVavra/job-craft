import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
  });

  it('shows a message for 2.6 seconds', () => {
    jasmine.clock().install();
    const toast = TestBed.inject(ToastService);
    toast.show('Briefkopf gespeichert');
    expect(toast.message()).toBe('Briefkopf gespeichert');
    jasmine.clock().tick(2599);
    expect(toast.message()).toBe('Briefkopf gespeichert');
    jasmine.clock().tick(1);
    expect(toast.message()).toBeNull();
    jasmine.clock().uninstall();
  });

  it('restarts the timer for a new message', () => {
    jasmine.clock().install();
    const toast = TestBed.inject(ToastService);
    toast.show('Eins');
    jasmine.clock().tick(2000);
    toast.show('Zwei');
    jasmine.clock().tick(2000);
    expect(toast.message()).toBe('Zwei');
    jasmine.clock().tick(600);
    expect(toast.message()).toBeNull();
    jasmine.clock().uninstall();
  });
});
