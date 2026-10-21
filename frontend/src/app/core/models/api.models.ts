/** API-Verträge zwischen Angular-Frontend und Spring-Boot-Backend. */

export type LetterStyle = 'CLASSIC' | 'MODERN';
export type DocumentCategory = 'CV' | 'SKILLS' | 'CERTIFICATE' | 'OTHER';
export type DocumentStatus = 'PROCESSING' | 'READY' | 'FAILED';
export type ApplicationStatus =
  'ANALYZING' | 'ANALYSIS_FAILED' | 'ANALYZED' | 'DRAFT' | 'SENT' | 'INTERVIEW' | 'REJECTED';
export type Tone = 'FORMAL' | 'MODERN' | 'CASUAL';
export type LetterLength = 'COMPACT' | 'STANDARD' | 'DETAILED';
export type ChatRole = 'USER' | 'ASSISTANT' | 'SYSTEM';

export interface User {
  id: string;
  email: string;
  name: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  name: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface CustomField {
  label: string;
  value: string;
}

export interface Profile {
  fullName: string;
  street: string | null;
  zipCode: string | null;
  city: string | null;
  phone: string | null;
  email: string | null;
  linkedin: string | null;
  github: string | null;
  letterStyle: LetterStyle;
  customFields: CustomField[];
}

/** Weitere Angabe zur Person („Über mich“), z. B. ein Privatprojekt. */
export interface ProfileNote {
  id: string;
  title: string;
  content: string;
  /** Fließt in Analyse und Anschreiben ein. */
  included: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProfileNoteRequest {
  title: string;
  content: string;
  included: boolean;
}

export interface DocumentSummary {
  id: string;
  fileName: string;
  contentType: string;
  category: DocumentCategory;
  sizeBytes: number;
  pageCount: number | null;
  status: DocumentStatus;
  createdAt: string;
}

export interface DocumentDetail extends DocumentSummary {
  extractedText: string | null;
}

export interface Requirement {
  text: string;
  matched: boolean;
}

export interface LetterMeta {
  senderName: string;
  senderLine: string;
  recipientLines: string[];
  dateLine: string;
  subject: string;
  salutation: string;
  closing: string;
}

export interface CoverLetterVersion {
  version: number;
  paragraphs: string[];
  changedParagraphs: number[];
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  role: ChatRole;
  message: string;
  meta: string | null;
  coverLetterVersion: number | null;
  createdAt: string;
}

export interface ApplicationSummary {
  id: string;
  companyName: string | null;
  jobTitle: string | null;
  status: ApplicationStatus;
  createdAt: string;
  latestVersion: number;
}

export interface ApplicationDetail {
  id: string;
  jobUrl: string;
  jobTitle: string | null;
  companyName: string | null;
  contactPerson: string | null;
  recipientAddress: string | null;
  requirements: Requirement[];
  tasks: string[];
  contextDocumentIds: string[];
  tone: Tone;
  length: LetterLength;
  status: ApplicationStatus;
  analysisStep: number;
  errorMessage: string | null;
  createdAt: string;
  letter: LetterMeta;
  versions: CoverLetterVersion[];
  messages: ChatMessage[];
}

export interface GenerateRequest {
  companyName: string;
  jobTitle: string;
  contactPerson: string;
  recipientAddress: string;
  requirements: Requirement[];
  tasks: string[];
  documentIds: string[];
  tone: Tone;
  length: LetterLength;
}

export interface PdfExportOptions {
  version: number;
  template: LetterStyle;
  attachCv: boolean;
  attachCertificates: boolean;
}

export interface SystemInfo {
  aiEnabled: boolean;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fieldErrors: Record<string, string>;
}
