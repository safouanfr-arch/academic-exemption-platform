import {Component, Input} from '@angular/core';
import {MatCard, MatCardContent, MatCardTitle} from '@angular/material/card';
import {UeDetail} from '../../model/ue-detail';

@Component({
  selector: 'app-eu-detail',
  imports: [
    MatCard,
    MatCardTitle,
    MatCardContent
  ],
  templateUrl: './eu-detail.component.html',
  styleUrl: './eu-detail.component.css',
})
export class EuDetailComponent {
  @Input() ue!: UeDetail;
}
