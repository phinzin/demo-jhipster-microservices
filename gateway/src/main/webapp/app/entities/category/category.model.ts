export interface ICategory {
  id?: number;
  name?: string | null;
  description?: string | null;
  active?: boolean | null;
}

export class Category implements ICategory {
  constructor(public id?: number, public name?: string | null, public description?: string | null, public active?: boolean | null) {
    this.active = this.active ?? false;
  }
}

export function getCategoryIdentifier(category: ICategory): number | undefined {
  return category.id;
}
