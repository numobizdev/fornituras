using System.Text;
using Fornituras.Api.Common.Crypto;
using Fornituras.Api.Configuration;
using Fornituras.Api.Data;
using Fornituras.Api.Security;
using Fornituras.Api.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

namespace Fornituras.Api.Extensions;

public static class ServiceCollectionExtensions
{
    public static IServiceCollection AddForniturasServices(this IServiceCollection services, IConfiguration configuration)
    {
        services.Configure<AppOptions>(configuration.GetSection(AppOptions.SectionName));
        var appOptions = configuration.GetSection(AppOptions.SectionName).Get<AppOptions>() ?? new AppOptions();

        var connectionString = configuration.GetConnectionString("Default")
            ?? configuration.GetConnectionString("DefaultConnection");

        services.AddDbContext<ApplicationDbContext>(options =>
            options.UseSqlServer(connectionString));

        services.AddHttpContextAccessor();

        if (!string.IsNullOrWhiteSpace(appOptions.Pii.EncryptionKey))
        {
            PiiCipher.Configure(Encoding.UTF8.GetBytes(appOptions.Pii.EncryptionKey));
        }

        services.AddSingleton(new BlindIndexer(appOptions.Pii.BlindIndexKey));

        services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer(options =>
            {
                options.TokenValidationParameters = new TokenValidationParameters
                {
                    ValidateIssuer = false,
                    ValidateAudience = false,
                    ValidateLifetime = true,
                    ValidateIssuerSigningKey = true,
                    IssuerSigningKey = new SymmetricSecurityKey(JwtTokenService.ResolveKeyBytes(appOptions.Jwt.Secret)),
                    RoleClaimType = System.Security.Claims.ClaimTypes.Role,
                    ClockSkew = TimeSpan.FromMinutes(1)
                };
            });

        services.AddAuthorization();

        services.AddCors(options =>
        {
            options.AddDefaultPolicy(policy =>
            {
                var origins = appOptions.Cors.AllowedOrigins;
                if (origins.Count == 0)
                {
                    origins = ["http://localhost:8100"];
                }

                policy.WithOrigins(origins.ToArray())
                    .AllowAnyHeader()
                    .AllowAnyMethod()
                    .AllowCredentials();
            });
        });

        services.AddScoped<JwtTokenService>();
        services.AddScoped<CurrentUserService>();
        services.AddScoped<IAuditWriter, AuditWriter>();
        services.AddScoped<LoginAttemptService>();
        services.AddScoped<EmailService>();

        services.AddScoped<AuthService>();
        services.AddScoped<IAuthService>(sp => sp.GetRequiredService<AuthService>());
        services.AddScoped<UserService>();
        services.AddScoped<IUserService>(sp => sp.GetRequiredService<UserService>());
        services.AddScoped<CatalogService>();
        services.AddScoped<ICatalogService>(sp => sp.GetRequiredService<CatalogService>());
        services.AddScoped<WarehouseService>();
        services.AddScoped<IWarehouseService>(sp => sp.GetRequiredService<WarehouseService>());
        services.AddScoped<EquipmentService>();
        services.AddScoped<IEquipmentService>(sp => sp.GetRequiredService<EquipmentService>());
        services.AddScoped<OfficerService>();
        services.AddScoped<IOfficerService>(sp => sp.GetRequiredService<OfficerService>());
        services.AddScoped<AssignmentService>();
        services.AddScoped<IAssignmentService>(sp => sp.GetRequiredService<AssignmentService>());
        services.AddScoped<QrCodeGeneratorService>();
        services.AddScoped<LoteQrService>();
        services.AddScoped<QrPdfService>();
        services.AddScoped<QrZipService>();
        services.AddScoped<IQrService, QrService>();
        services.AddScoped<IDashboardService, DashboardService>();
        services.AddScoped<ITransferService, TransferService>();
        services.AddScoped<IncidentService>();
        services.AddScoped<IIncidentService>(sp => sp.GetRequiredService<IncidentService>());
        services.AddScoped<AlertService>();
        services.AddScoped<IAlertService, AlertServiceAdapter>();
        services.AddScoped<DecommissionService>();
        services.AddScoped<IDecommissionService, DecommissionServiceAdapter>();
        services.AddScoped<ReportService>();
        services.AddScoped<IReportService, ReportServiceAdapter>();
        services.AddScoped<AuditLogService>();
        services.AddScoped<IAuditService, AuditService>();
        services.AddScoped<LandingService>();
        services.AddScoped<ILandingService, LandingServiceAdapter>();
        services.AddScoped<DataSeeder>();

        services.AddControllers();
        services.AddOpenApi();
        services.AddHealthChecks();

        return services;
    }
}
