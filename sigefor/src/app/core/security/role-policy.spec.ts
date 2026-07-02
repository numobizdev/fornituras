import { ROLE_POLICY } from './role-policy';

/**
 * La matriz DEBE ser espejo exacto de RolePolicy.cs (backend, ADR 0013).
 * Contrato: specs/021-rbac-ui-validacion-rediseno/contracts/ui-permissions.md
 */
describe('ROLE_POLICY (espejo de RolePolicy.cs)', () => {
  it('WRITE_INVENTORY = ADMIN, ALMACEN, CAPTURISTA', () => {
    expect([...ROLE_POLICY.WRITE_INVENTORY].sort()).toEqual(['ADMIN', 'ALMACEN', 'CAPTURISTA']);
  });

  it('WRITE_TRANSFERS = ADMIN, SUPERVISOR, ALMACEN, CAPTURISTA', () => {
    expect([...ROLE_POLICY.WRITE_TRANSFERS].sort()).toEqual([
      'ADMIN',
      'ALMACEN',
      'CAPTURISTA',
      'SUPERVISOR',
    ]);
  });

  it('WRITE_OPERATIONS = ADMIN, SUPERVISOR, CAPTURISTA', () => {
    expect([...ROLE_POLICY.WRITE_OPERATIONS].sort()).toEqual(['ADMIN', 'CAPTURISTA', 'SUPERVISOR']);
  });

  it('AUTHORIZE_DECOMMISSION = ADMIN, SUPERVISOR', () => {
    expect([...ROLE_POLICY.AUTHORIZE_DECOMMISSION].sort()).toEqual(['ADMIN', 'SUPERVISOR']);
  });

  it('WRITE_OFFICERS = ADMIN, SUPERVISOR, CAPTURISTA', () => {
    expect([...ROLE_POLICY.WRITE_OFFICERS].sort()).toEqual(['ADMIN', 'CAPTURISTA', 'SUPERVISOR']);
  });

  it('MANAGE_CONFIG, MANAGE_LANDING y MANAGE_USERS = solo ADMIN', () => {
    expect([...ROLE_POLICY.MANAGE_CONFIG]).toEqual(['ADMIN']);
    expect([...ROLE_POLICY.MANAGE_LANDING]).toEqual(['ADMIN']);
    expect([...ROLE_POLICY.MANAGE_USERS]).toEqual(['ADMIN']);
  });

  it('READ_AUDIT = ADMIN, AUDITOR', () => {
    expect([...ROLE_POLICY.READ_AUDIT].sort()).toEqual(['ADMIN', 'AUDITOR']);
  });

  it('MANAGE_QR_LOTES = SUPER_ADMIN', () => {
    expect([...ROLE_POLICY.MANAGE_QR_LOTES]).toEqual(['SUPER_ADMIN']);
  });
});
