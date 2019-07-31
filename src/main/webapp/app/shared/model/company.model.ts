import { IPerson } from 'app/shared/model/person.model';

export interface ICompany {
  id?: number;
  name?: string;
  isActive?: boolean;
  ownerPersonId?: IPerson;
}

export const defaultValue: Readonly<ICompany> = {
  isActive: false
};
