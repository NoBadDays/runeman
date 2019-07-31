import { ICompany } from 'app/shared/model/company.model';
import { IMeasure } from 'app/shared/model/measure.model';

export interface IProduct {
  id?: number;
  name?: string;
  barcode?: string;
  quantity?: number;
  price?: number;
  companyId?: ICompany;
  measureId?: IMeasure;
}

export const defaultValue: Readonly<IProduct> = {};
