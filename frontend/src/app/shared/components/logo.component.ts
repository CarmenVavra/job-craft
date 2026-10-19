import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** JobCraft-Logo: Teal-Quadrat mit „J“ und rotem Punkt. */
@Component({
  selector: 'app-logo',
  template: `
    <span
      class="mark"
      [style.width.px]="size()"
      [style.height.px]="size()"
      [style.fontSize.px]="size() * 0.53"
    >
      J<span class="dot" [style.width.px]="size() * 0.3" [style.height.px]="size() * 0.3"></span>
    </span>
    <span class="word" [style.fontSize.px]="size() * 0.6">JobCraft</span>
  `,
  styles: `
    :host {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .mark {
      border-radius: 8px;
      background: var(--jc-teal);
      display: grid;
      place-items: center;
      font-weight: 700;
      color: #fff;
      position: relative;
      flex: none;
    }
    .dot {
      position: absolute;
      right: -3px;
      top: -3px;
      border-radius: 50%;
      background: var(--jc-red);
      border: 2px solid var(--jc-ink);
    }
    .word {
      font-weight: 700;
      letter-spacing: -0.01em;
      color: #fff;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogoComponent {
  readonly size = input(30);
}
