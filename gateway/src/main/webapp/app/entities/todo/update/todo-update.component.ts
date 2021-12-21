import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import { ITodo, Todo } from '../todo.model';
import { TodoService } from '../service/todo.service';
import { ICategory } from 'app/entities/category/category.model';
import { CategoryService } from 'app/entities/category/service/category.service';

@Component({
  selector: 'jhi-todo-update',
  templateUrl: './todo-update.component.html',
})
export class TodoUpdateComponent implements OnInit {
  isSaving = false;

  categoriesCollection: ICategory[] = [];

  editForm = this.fb.group({
    id: [],
    task: [],
    description: [],
    completed: [],
    category: [],
  });

  constructor(
    protected todoService: TodoService,
    protected categoryService: CategoryService,
    protected activatedRoute: ActivatedRoute,
    protected fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ todo }) => {
      this.updateForm(todo);

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const todo = this.createFromForm();
    if (todo.id !== undefined) {
      this.subscribeToSaveResponse(this.todoService.update(todo));
    } else {
      this.subscribeToSaveResponse(this.todoService.create(todo));
    }
  }

  trackCategoryById(index: number, item: ICategory): number {
    return item.id!;
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ITodo>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError()
    );
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(todo: ITodo): void {
    this.editForm.patchValue({
      id: todo.id,
      task: todo.task,
      description: todo.description,
      completed: todo.completed,
      category: todo.category,
    });

    this.categoriesCollection = this.categoryService.addCategoryToCollectionIfMissing(this.categoriesCollection, todo.category);
  }

  protected loadRelationshipsOptions(): void {
    this.categoryService
      .query({ filter: 'todo-is-null' })
      .pipe(map((res: HttpResponse<ICategory[]>) => res.body ?? []))
      .pipe(
        map((categories: ICategory[]) =>
          this.categoryService.addCategoryToCollectionIfMissing(categories, this.editForm.get('category')!.value)
        )
      )
      .subscribe((categories: ICategory[]) => (this.categoriesCollection = categories));
  }

  protected createFromForm(): ITodo {
    return {
      ...new Todo(),
      id: this.editForm.get(['id'])!.value,
      task: this.editForm.get(['task'])!.value,
      description: this.editForm.get(['description'])!.value,
      completed: this.editForm.get(['completed'])!.value,
      category: this.editForm.get(['category'])!.value,
    };
  }
}
