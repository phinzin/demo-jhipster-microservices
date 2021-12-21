import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: 'todo',
        data: { pageTitle: 'gatewayApp.todo.home.title' },
        loadChildren: () => import('./todo/todo.module').then(m => m.TodoModule),
      },
      {
        path: 'category',
        data: { pageTitle: 'gatewayApp.category.home.title' },
        loadChildren: () => import('./category/category.module').then(m => m.CategoryModule),
      },
      /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
    ]),
  ],
})
export class EntityRoutingModule {}
