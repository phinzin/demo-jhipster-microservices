jest.mock('@angular/router');

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { CategoryService } from '../service/category.service';

import { CategoryComponent } from './category.component';

describe('Category Management Component', () => {
  let comp: CategoryComponent;
  let fixture: ComponentFixture<CategoryComponent>;
  let service: CategoryService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [CategoryComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParams: {} } },
        },
      ],
    })
      .overrideTemplate(CategoryComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(CategoryComponent);
    comp = fixture.componentInstance;
    service = TestBed.inject(CategoryService);

    const headers = new HttpHeaders();
    jest.spyOn(service, 'query').mockReturnValue(
      of(
        new HttpResponse({
          body: [{ id: 123 }],
          headers,
        })
      )
    );
  });

  it('Should call load all on init', () => {
    // WHEN
    comp.ngOnInit();

    // THEN
    expect(service.query).toHaveBeenCalled();
    expect(comp.categories?.[0]).toEqual(expect.objectContaining({ id: 123 }));
  });
});
