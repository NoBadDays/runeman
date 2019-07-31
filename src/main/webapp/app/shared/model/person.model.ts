export interface IPerson {
  id?: number;
  name?: string;
  surname?: string;
  email?: string;
  cell?: string;
}

export const defaultValue: Readonly<IPerson> = {};
