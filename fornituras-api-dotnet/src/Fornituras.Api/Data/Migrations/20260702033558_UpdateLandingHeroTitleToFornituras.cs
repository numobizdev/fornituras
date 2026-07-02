using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Fornituras.Api.Data.Migrations
{
    /// <summary>
    /// Actualiza el título del HERO público de la landing a la identidad oficial
    /// "Sistema Integral de Gestión de Fornituras" (ADR 0020). Se acota al valor sembrado
    /// anterior exacto para no sobrescribir ediciones deliberadas del administrador; por el
    /// mismo filtro la operación es idempotente.
    /// </summary>
    public partial class UpdateLandingHeroTitleToFornituras : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql(
                """
                UPDATE landing_section
                SET titulo = N'Sistema Integral de Gestión de Fornituras',
                    updated_at = SYSUTCDATETIME()
                WHERE scope = 'PUBLIC'
                  AND type = 'HERO'
                  AND titulo = N'Sistema de Gestión de Blindajes';
                """);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql(
                """
                UPDATE landing_section
                SET titulo = N'Sistema de Gestión de Blindajes',
                    updated_at = SYSUTCDATETIME()
                WHERE scope = 'PUBLIC'
                  AND type = 'HERO'
                  AND titulo = N'Sistema Integral de Gestión de Fornituras';
                """);
        }
    }
}
