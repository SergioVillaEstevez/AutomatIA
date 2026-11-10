import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiService } from '../../core/api.service';
import { formatWhen, hospStatusLabel } from '../../core/labels';
import { FloorPlan, HospitalityResource } from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-hosp-floor-page',
  imports: [MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './floor-page.html',
  styles: `
    .floor {
      position: relative;
      border: 1px solid var(--line);
      border-radius: 12px;
      background:
        linear-gradient(90deg, rgba(15, 39, 68, 0.04) 1px, transparent 1px) 0 0 / 40px 40px,
        linear-gradient(rgba(15, 39, 68, 0.04) 1px, transparent 1px) 0 0 / 40px 40px,
        #fbfcfe;
      margin: 16px 0;
      overflow: hidden;
    }
    .table {
      position: absolute;
      border: 2px solid #2f6fed;
      border-radius: 12px;
      background: rgba(47, 111, 237, 0.12);
      display: grid;
      place-content: center;
      text-align: center;
      cursor: grab;
      user-select: none;
      padding: 4px;
      font-size: 0.8rem;
      font-weight: 600;
    }
    .table.busy {
      border-color: #b42318;
      background: rgba(180, 35, 24, 0.12);
    }
    .table small { display: block; font-weight: 400; color: var(--muted); }
  `,
})
export class HospFloorPage {
  private readonly api = inject(ApiService);
  readonly day = signal(localDay());
  readonly plan = signal<FloorPlan | null>(null);
  readonly error = signal('');
  readonly formatWhen = formatWhen;
  readonly hospStatusLabel = hospStatusLabel;
  private drag: { id: string; ox: number; oy: number } | null = null;

  constructor() {
    this.reload();
  }

  changeDay(value: string): void {
    if (!value) return;
    this.day.set(value);
    this.reload();
  }

  reload(): void {
    this.api.hospFloorPlan(this.day()).subscribe({
      next: (plan) => this.plan.set(plan),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  busy(resourceId: string): boolean {
    return (this.plan()?.openReservations ?? []).some((item) => item.resourceId === resourceId);
  }

  onPointerDown(event: PointerEvent, resource: HospitalityResource): void {
    const target = event.currentTarget as HTMLElement;
    target.setPointerCapture(event.pointerId);
    this.drag = {
      id: resource.id,
      ox: event.clientX - resource.posX,
      oy: event.clientY - resource.posY,
    };
  }

  onPointerMove(event: PointerEvent, resource: HospitalityResource): void {
    if (!this.drag || this.drag.id !== resource.id || !this.plan()) {
      return;
    }
    const plan = this.plan()!;
    const x = Math.max(0, Math.min(plan.floorWidth - resource.width, Math.round(event.clientX - this.drag.ox)));
    const y = Math.max(0, Math.min(plan.floorHeight - resource.height, Math.round(event.clientY - this.drag.oy)));
    this.plan.set({
      ...plan,
      resources: plan.resources.map((item) => (item.id === resource.id ? { ...item, posX: x, posY: y } : item)),
    });
  }

  onPointerUp(resource: HospitalityResource): void {
    if (!this.drag || this.drag.id !== resource.id) {
      return;
    }
    this.drag = null;
    this.api.updateHospResource(resource.id, {
      name: resource.name,
      seats: resource.seats,
      sortOrder: resource.sortOrder,
      active: resource.active,
      posX: resource.posX,
      posY: resource.posY,
      width: resource.width,
      height: resource.height,
    }).subscribe({
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}

function localDay(): string {
  const madrid = new Date(new Date().toLocaleString('en-US', { timeZone: 'Europe/Madrid' }));
  return `${madrid.getFullYear()}-${String(madrid.getMonth() + 1).padStart(2, '0')}-${String(madrid.getDate()).padStart(2, '0')}`;
}
