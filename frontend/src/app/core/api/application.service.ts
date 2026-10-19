import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, switchMap, takeWhile, timer } from 'rxjs';
import {
  ApplicationDetail,
  ApplicationStatus,
  ApplicationSummary,
  GenerateRequest,
  PdfExportOptions,
} from '../models/api.models';

const POLL_INTERVAL_MS = 500;

/** Zugriff auf Bewerbungen: Analyse, Entwurf, Chat, Status und PDF-Export. */
@Injectable({ providedIn: 'root' })
export class ApplicationService {
  private readonly http = inject(HttpClient);

  list(): Observable<ApplicationSummary[]> {
    return this.http.get<ApplicationSummary[]>('/api/applications');
  }

  get(id: string): Observable<ApplicationDetail> {
    return this.http.get<ApplicationDetail>(`/api/applications/${id}`);
  }

  /** Startet Scraping und Analyse der Stellenanzeige. */
  create(url: string): Observable<ApplicationDetail> {
    return this.http.post<ApplicationDetail>('/api/applications', { url });
  }

  /** Fragt den Analysefortschritt ab, bis die Analyse beendet ist (letzter Wert inklusive). */
  watchAnalysis(id: string): Observable<ApplicationDetail> {
    return timer(0, POLL_INTERVAL_MS).pipe(
      switchMap(() => this.get(id)),
      takeWhile((app) => app.status === 'ANALYZING', true),
    );
  }

  generate(id: string, request: GenerateRequest): Observable<ApplicationDetail> {
    return this.http.post<ApplicationDetail>(`/api/applications/${id}/generate`, request);
  }

  chat(id: string, message: string): Observable<ApplicationDetail> {
    return this.http.post<ApplicationDetail>(`/api/applications/${id}/chat`, { message });
  }

  /** Speichert direkt bearbeitete oder gelöschte Absätze als neue Version. */
  updateParagraphs(id: string, paragraphs: string[]): Observable<ApplicationDetail> {
    return this.http.put<ApplicationDetail>(`/api/applications/${id}/paragraphs`, { paragraphs });
  }

  updateStatus(id: string, status: ApplicationStatus): Observable<ApplicationDetail> {
    return this.http.patch<ApplicationDetail>(`/api/applications/${id}/status`, { status });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/applications/${id}`);
  }

  pdf(id: string, options: PdfExportOptions): Observable<Blob> {
    const params = new HttpParams()
      .set('version', options.version)
      .set('template', options.template)
      .set('attachCv', options.attachCv)
      .set('attachCertificates', options.attachCertificates);
    return this.http.get(`/api/applications/${id}/pdf`, { params, responseType: 'blob' });
  }
}
