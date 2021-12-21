import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { ICategory } from '../category.model';
import { CategoryService } from '../service/category.service';
import { CategoryDeleteDialogComponent } from '../delete/category-delete-dialog.component';

@Component({
  selector: 'jhi-category',
  templateUrl: './category.component.html',
})
export class CategoryComponent implements OnInit {
  categories?: ICategory[];
  isLoading = false;
  currentSearch: string;

  constructor(protected categoryService: CategoryService, protected modalService: NgbModal, protected activatedRoute: ActivatedRoute) {
    this.currentSearch = this.activatedRoute.snapshot.queryParams['search'] ?? '';
  }

  loadAll(): void {
    this.isLoading = true;
    if (this.currentSearch) {
      this.categoryService
        .search({
          query: this.currentSearch,
        })
        .subscribe(
          (res: HttpResponse<ICategory[]>) => {
            this.isLoading = false;
            this.categories = res.body ?? [];
          },
          () => {
            this.isLoading = false;
          }
        );
      return;
    }

    this.categoryService.query().subscribe(
      (res: HttpResponse<ICategory[]>) => {
        this.isLoading = false;
        this.categories = res.body ?? [];
      },
      () => {
        this.isLoading = false;
      }
    );
  }

  search(query: string): void {
    this.currentSearch = query;
    this.loadAll();
  }

  ngOnInit(): void {
    this.loadAll();
  }

  trackId(index: number, item: ICategory): number {
    return item.id!;
  }

  delete(category: ICategory): void {
    const modalRef = this.modalService.open(CategoryDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.category = category;
    // unsubscribe not needed because closed completes on modal close
    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.loadAll();
      }
    });
  }
}
