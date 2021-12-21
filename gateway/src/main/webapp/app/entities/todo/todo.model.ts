import { ICategory } from 'app/entities/category/category.model';

export interface ITodo {
  id?: number;
  task?: string | null;
  description?: string | null;
  completed?: boolean | null;
  category?: ICategory | null;
}

export class Todo implements ITodo {
  constructor(
    public id?: number,
    public task?: string | null,
    public description?: string | null,
    public completed?: boolean | null,
    public category?: ICategory | null
  ) {
    this.completed = this.completed ?? false;
  }
}

export function getTodoIdentifier(todo: ITodo): number | undefined {
  return todo.id;
}
