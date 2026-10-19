import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { EnvironmentProviders, Provider, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

/** Standard-Provider für Komponententests (zoneless, HTTP-Testing, Router). */
export function testProviders(): (Provider | EnvironmentProviders)[] {
  return [
    provideZonelessChangeDetection(),
    provideHttpClient(),
    provideHttpClientTesting(),
    provideRouter([]),
  ];
}

/** Liefert den sichtbaren Text eines Elements ohne Mehrfach-Leerzeichen. */
export function text(element: Element | null): string {
  return (element?.textContent ?? '').replace(/\s+/g, ' ').trim();
}
