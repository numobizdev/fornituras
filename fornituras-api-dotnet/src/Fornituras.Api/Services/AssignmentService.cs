using Fornituras.Api.Common;
using Fornituras.Api.Common.Crypto;
using Fornituras.Api.Data;
using Fornituras.Api.Data.Entities;
using Fornituras.Api.Dto;
using Fornituras.Api.Security;
using Microsoft.EntityFrameworkCore;
using EntityEquipmentStatus = Fornituras.Api.Data.Entities.EquipmentStatus;

namespace Fornituras.Api.Services;

public sealed class AssignmentService(
    ApplicationDbContext db,
    CurrentUserService currentUser,
    IAuditWriter audit,
    ILogger<AssignmentService> logger) : IAssignmentService
{
    public Task<PageResult<AssignmentSummary>> FindAllAsync(
        PaginationQuery pagination,
        CancellationToken cancellationToken = default) =>
        FindVigentesAsync(pagination.Page, pagination.Size, cancellationToken);

    public async Task<PageResult<AssignmentSummary>> FindVigentesAsync(
        int page,
        int size,
        CancellationToken cancellationToken = default)
    {
        var query = db.Assignments.AsNoTracking()
            .Where(a => a.FechaDevolucion == null)
            .OrderByDescending(a => a.FechaAsignacion);

        var total = await query.CountAsync(cancellationToken);
        var assignments = await query.Skip(page * size).Take(size).ToListAsync(cancellationToken);
        var summaries = await MapSummariesAsync(assignments, cancellationToken);
        return PageResult<AssignmentSummary>.From(summaries, total, page, size);
    }

    public async Task<AssignmentSummary> AssignAsync(
        AssignRequest request,
        CancellationToken cancellationToken = default)
    {
        var equipment = await db.Equipment.FirstOrDefaultAsync(e => e.Id == request.EquipmentId, cancellationToken)
            ?? throw new NotFoundException($"Equipment not found: {request.EquipmentId}");

        if (equipment.Status != EntityEquipmentStatus.DISPONIBLE)
        {
            throw new ConflictException("La fornitura no está disponible para asignación.");
        }

        var officer = await db.Officers.FirstOrDefaultAsync(o => o.Id == request.OfficerId, cancellationToken)
            ?? throw new NotFoundException($"Officer not found: {request.OfficerId}");

        if (!officer.Active)
        {
            throw new BadRequestException("El elemento no está activo.");
        }

        if (await db.Assignments.AnyAsync(
                a => a.EquipmentId == request.EquipmentId && a.FechaDevolucion == null,
                cancellationToken))
        {
            throw new ConflictException("La fornitura ya tiene una asignación vigente.");
        }

        var userId = await currentUser.GetUserIdAsync(cancellationToken);
        var now = DateTime.UtcNow;

        var assignment = new Assignment
        {
            EquipmentId = request.EquipmentId,
            OfficerId = request.OfficerId,
            FechaAsignacion = now,
            AsignadoPor = userId,
            Observaciones = request.Observaciones?.Trim(),
            CreatedAt = now,
            UpdatedAt = now
        };

        equipment.Status = EntityEquipmentStatus.ASIGNADA;
        equipment.UpdatedAt = now;

        db.Assignments.Add(assignment);

        try
        {
            await db.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException ex)
        {
            logger.LogWarning(ex, "Conflict on assign equipment {EquipmentId}", request.EquipmentId);
            throw new ConflictException("Conflicto de concurrencia al asignar la fornitura.");
        }

        await audit.RecordAsync("ASSIGN", assignment.Id, cancellationToken);
        return (await MapSummariesAsync([assignment], cancellationToken)).Single();
    }

    public Task<AssignmentSummary> ReturnAsync(long id, CancellationToken cancellationToken = default) =>
        ReturnAssignmentAsync(id, cancellationToken);

    public async Task<AssignmentSummary> ReturnAssignmentAsync(
        long id,
        CancellationToken cancellationToken = default)
    {
        var assignment = await db.Assignments
            .FirstOrDefaultAsync(a => a.Id == id, cancellationToken)
            ?? throw new NotFoundException($"Assignment not found: {id}");

        if (assignment.FechaDevolucion is not null)
        {
            throw new ConflictException("La asignación ya fue devuelta.");
        }

        var equipment = await db.Equipment.FirstOrDefaultAsync(e => e.Id == assignment.EquipmentId, cancellationToken)
            ?? throw new NotFoundException($"Equipment not found: {assignment.EquipmentId}");

        var userId = await currentUser.GetUserIdAsync(cancellationToken);
        var now = DateTime.UtcNow;

        assignment.FechaDevolucion = now;
        assignment.RecibidoPor = userId;
        assignment.UpdatedAt = now;

        equipment.Status = EntityEquipmentStatus.DISPONIBLE;
        equipment.UpdatedAt = now;

        await db.SaveChangesAsync(cancellationToken);
        await audit.RecordAsync("RETURN", assignment.Id, cancellationToken);
        return (await MapSummariesAsync([assignment], cancellationToken)).Single();
    }

    public async Task<AssignmentSummary> ReassignAsync(
        ReassignRequest request,
        CancellationToken cancellationToken = default)
    {
        var current = await db.Assignments
            .FirstOrDefaultAsync(
                a => a.EquipmentId == request.EquipmentId && a.FechaDevolucion == null,
                cancellationToken)
            ?? throw new NotFoundException("No hay asignación vigente para esta fornitura.");

        var newOfficer = await db.Officers.FirstOrDefaultAsync(o => o.Id == request.NewOfficerId, cancellationToken)
            ?? throw new NotFoundException($"Officer not found: {request.NewOfficerId}");

        if (!newOfficer.Active)
        {
            throw new BadRequestException("El elemento no está activo.");
        }

        var userId = await currentUser.GetUserIdAsync(cancellationToken);
        var now = DateTime.UtcNow;

        current.FechaDevolucion = now;
        current.RecibidoPor = userId;
        current.UpdatedAt = now;
        await db.SaveChangesAsync(cancellationToken);

        var newAssignment = new Assignment
        {
            EquipmentId = request.EquipmentId,
            OfficerId = request.NewOfficerId,
            FechaAsignacion = now,
            AsignadoPor = userId,
            Observaciones = request.Observaciones?.Trim(),
            CreatedAt = now,
            UpdatedAt = now
        };

        db.Assignments.Add(newAssignment);

        try
        {
            await db.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException ex)
        {
            logger.LogWarning(ex, "Conflict on reassign equipment {EquipmentId}", request.EquipmentId);
            throw new ConflictException("Conflicto de concurrencia al reasignar la fornitura.");
        }

        await audit.RecordAsync("REASSIGN", newAssignment.Id, cancellationToken);
        return (await MapSummariesAsync([newAssignment], cancellationToken)).Single();
    }

    private async Task<IReadOnlyList<AssignmentSummary>> MapSummariesAsync(
        IReadOnlyList<Assignment> assignments,
        CancellationToken cancellationToken)
    {
        var equipmentIds = assignments.Select(a => a.EquipmentId).Distinct().ToList();
        var officerIds = assignments.Select(a => a.OfficerId).Distinct().ToList();

        var equipmentMap = await db.Equipment.AsNoTracking()
            .Where(e => equipmentIds.Contains(e.Id))
            .ToDictionaryAsync(e => e.Id, cancellationToken);

        var officers = await db.Officers.AsNoTracking()
            .Where(o => officerIds.Contains(o.Id))
            .ToListAsync(cancellationToken);

        var officerNames = officers.ToDictionary(
            o => o.Id,
            o => string.Join(" ",
                new[] { PiiCipher.Decrypt(o.Nombre), PiiCipher.Decrypt(o.ApellidoPaterno) }
                    .Where(s => !string.IsNullOrWhiteSpace(s))));

        var officerPlacas = officers.ToDictionary(o => o.Id, o => o.Placa);

        return assignments.Select(a =>
        {
            equipmentMap.TryGetValue(a.EquipmentId, out var equipment);
            return new AssignmentSummary(
                a.Id,
                a.EquipmentId,
                equipment?.CodigoQr ?? "?",
                equipment?.Descripcion,
                a.OfficerId,
                officerNames.GetValueOrDefault(a.OfficerId, "?"),
                officerPlacas.GetValueOrDefault(a.OfficerId, "?"),
                a.FechaAsignacion,
                a.FechaDevolucion,
                a.FechaDevolucion is null);
        }).ToList();
    }
}
