import { HttpErrorResponse } from '@angular/common/http';
import { profileFixture } from '../../../testing/fixtures';
import {
  cleanRole,
  errorMessage,
  formatSize,
  initials,
  pdfFileName,
  profileCompleteness,
} from './labels';

describe('labels utilities', () => {
  it('removes gender suffixes from roles', () => {
    expect(cleanRole('Senior Fullstack Entwickler (m/w/d)')).toBe('Senior Fullstack Entwickler');
    expect(cleanRole(null)).toBe('');
  });

  it('formats file sizes in German', () => {
    expect(formatSize(253952)).toBe('248 KB');
    expect(formatSize(1153434)).toBe('1,1 MB');
  });

  it('computes the letterhead completeness', () => {
    expect(profileCompleteness(profileFixture()).percent).toBe(100);
    expect(profileCompleteness(profileFixture({ github: null, linkedin: '' }))).toEqual({
      percent: 71,
      missing: 2,
    });
    expect(profileCompleteness(null).missing).toBe(7);
  });

  it('builds the PDF file name like the prototype', () => {
    expect(pdfFileName('Quantis Software GmbH', 'Lena Hoffmann')).toBe(
      'Bewerbung_Quantis_Software_GmbH_Hoffmann.pdf',
    );
    expect(pdfFileName('Brückner & Partner', 'Lena Hoffmann')).toBe(
      'Bewerbung_Brückner_Partner_Hoffmann.pdf',
    );
  });

  it('derives initials', () => {
    expect(initials('Lena Hoffmann')).toBe('LH');
  });

  it('reads API error messages', () => {
    const error = new HttpErrorResponse({ status: 400, error: { message: 'Ungültig.' } });
    expect(errorMessage(error)).toBe('Ungültig.');
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toBe(
      'Der Server ist nicht erreichbar.',
    );
    expect(errorMessage(new Error('x'), 'Fallback')).toBe('Fallback');
  });
});
