import { UserSummary } from './auth.model';

export interface NavItem {
  title: string;
  url: string;
  icon: string;
  /** Roles que pueden ver la entrada; si se omite, la ve cualquier usuario autenticado. */
  roles?: UserSummary['role'][];
}
