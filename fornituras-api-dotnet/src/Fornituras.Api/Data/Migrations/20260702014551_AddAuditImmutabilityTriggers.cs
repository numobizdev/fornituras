using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Fornituras.Api.Data.Migrations
{
    /// <inheritdoc />
    public partial class AddAuditImmutabilityTriggers : Migration
    {
        // Inmutabilidad de audit_log a nivel de BD (ADR 0012; remedia B-2 de la auditoría 018).
        // Equivalente a los triggers de la migración Java V21. CREATE TRIGGER debe ser la única
        // sentencia de su lote en SQL Server, por eso cada uno va en su propio Sql().

        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql(
                @"CREATE TRIGGER trg_audit_log_no_update ON audit_log INSTEAD OF UPDATE AS
BEGIN
    RAISERROR('audit_log es append-only: UPDATE no permitido.', 16, 1);
END;");

            migrationBuilder.Sql(
                @"CREATE TRIGGER trg_audit_log_no_delete ON audit_log INSTEAD OF DELETE AS
BEGIN
    RAISERROR('audit_log es append-only: DELETE no permitido.', 16, 1);
END;");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("DROP TRIGGER IF EXISTS trg_audit_log_no_update;");
            migrationBuilder.Sql("DROP TRIGGER IF EXISTS trg_audit_log_no_delete;");
        }
    }
}
