jest.mock('@angular/router');

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject } from 'rxjs';

import { TodoService } from '../service/todo.service';
import { ITodo, Todo } from '../todo.model';
import { ICategory } from 'app/entities/category/category.model';
import { CategoryService } from 'app/entities/category/service/category.service';

import { TodoUpdateComponent } from './todo-update.component';

describe('Todo Management Update Component', () => {
  let comp: TodoUpdateComponent;
  let fixture: ComponentFixture<TodoUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let todoService: TodoService;
  let categoryService: CategoryService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [TodoUpdateComponent],
      providers: [FormBuilder, ActivatedRoute],
    })
      .overrideTemplate(TodoUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(TodoUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    todoService = TestBed.inject(TodoService);
    categoryService = TestBed.inject(CategoryService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call category query and add missing value', () => {
      const todo: ITodo = { id: 456 };
      const category: ICategory = { id: 56076 };
      todo.category = category;

      const categoryCollection: ICategory[] = [{ id: 762 }];
      jest.spyOn(categoryService, 'query').mockReturnValue(of(new HttpResponse({ body: categoryCollection })));
      const expectedCollection: ICategory[] = [category, ...categoryCollection];
      jest.spyOn(categoryService, 'addCategoryToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ todo });
      comp.ngOnInit();

      expect(categoryService.query).toHaveBeenCalled();
      expect(categoryService.addCategoryToCollectionIfMissing).toHaveBeenCalledWith(categoryCollection, category);
      expect(comp.categoriesCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const todo: ITodo = { id: 456 };
      const category: ICategory = { id: 75298 };
      todo.category = category;

      activatedRoute.data = of({ todo });
      comp.ngOnInit();

      expect(comp.editForm.value).toEqual(expect.objectContaining(todo));
      expect(comp.categoriesCollection).toContain(category);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<Todo>>();
      const todo = { id: 123 };
      jest.spyOn(todoService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ todo });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: todo }));
      saveSubject.complete();

      // THEN
      expect(comp.previousState).toHaveBeenCalled();
      expect(todoService.update).toHaveBeenCalledWith(todo);
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<Todo>>();
      const todo = new Todo();
      jest.spyOn(todoService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ todo });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: todo }));
      saveSubject.complete();

      // THEN
      expect(todoService.create).toHaveBeenCalledWith(todo);
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<Todo>>();
      const todo = { id: 123 };
      jest.spyOn(todoService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ todo });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(todoService.update).toHaveBeenCalledWith(todo);
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Tracking relationships identifiers', () => {
    describe('trackCategoryById', () => {
      it('Should return tracked Category primary key', () => {
        const entity = { id: 123 };
        const trackResult = comp.trackCategoryById(0, entity);
        expect(trackResult).toEqual(entity.id);
      });
    });
  });
});
