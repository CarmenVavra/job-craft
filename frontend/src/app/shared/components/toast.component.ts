import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from '../../core/ui/toast.service';

/** Zeigt die aktuelle Toast-Meldung unten mittig an. */
@Component({
  selector: 'app-toast',
  template: `
    @if (toast.message(); as message) {
      <div class="toast" role="status" aria-live="polite">
        <span class="dot"></span>{{ message }}
      </div>
    }
  `,
  styles: `
    .toast {
      position: fixed;
      left: 50%;
      bottom: 28px;
      transform: translateX(-50%);
      background: var(--jc-ink);
      color: #fff;
      border-radius: 10px;
      padding: 12px 18px;
      font-size: 14px;
      font-weight: 500;
      box-shadow: 0 10px 30px rgba(47, 47, 40, 0.25);
      z-index: 1100;
      animation: jc-in 0.2s ease-out;
      display: flex;
      gap: 10px;
      align-items: center;
    }
    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--jc-teal);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastComponent {
  protected readonly toast = inject(ToastService);
}
