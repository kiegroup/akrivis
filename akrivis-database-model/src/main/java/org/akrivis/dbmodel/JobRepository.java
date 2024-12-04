package org.akrivis.dbmodel;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JobRepository implements PanacheRepository<Job> {

    public List<Job> findActiveJobs() {
        return list("status", Sort.ascending("id"), JobStatus.SCHEDULED);
    }

    public List<Job> findDraftJobs() {
        return list("status", JobStatus.DRAFT);
    }

    public List<RawData> findRawDataByJobId(Long jobId) {
        return getEntityManager().createQuery("select r from RawData r where job.id = :jobId order by r.id desc",
                        RawData.class)
                .setParameter("jobId", jobId)
                .getResultList();
    }

    public Optional<RawData> findLatestRawDataByEndPoint(final String endpoint) {
        try {

            final Job job = getEntityManager().createQuery(
                            "select j from Job j where endpoint = :endPoint",
                            Job.class)
                    .setParameter("endPoint", endpoint)
                    .getSingleResult();

            final Instant maxCreatedAt = getEntityManager().createQuery(
                            "select max(createdAt) from RawData where job.id = :jobId",
                            Instant.class)
                    .setParameter("jobId", job.id)
                    .getSingleResult();

            final RawData rawData = getEntityManager().createQuery(
                            "select r from RawData r where job.id = :jobId and createdAt = :createdAt",
                            RawData.class)
                    .setParameter("jobId", job.id)
                    .setParameter("createdAt", maxCreatedAt)
                    .getSingleResult();

            return Optional.of(rawData);

        } catch (NoResultException | NonUniqueResultException e) {
            return Optional.empty();
        }
    }

    public Optional<RawData> findLatestRawDataByJobId(Long jobId) {
        try {
            return Optional.of(getEntityManager().createQuery(
                            """
                                    select r from RawData r where job.id = :jobId and createdAt =
                                    (select max(createdAt) from RawData where job.id = :jobId)
                            """,
                            RawData.class)
                    .setParameter("jobId", jobId)
                    .getSingleResult());
        } catch (NoResultException | NonUniqueResultException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<RawData> findRawDataById(Long jobId, Long rawDataId) {

        try {
            return Optional.of(getEntityManager().createQuery("select r from RawData r where r.id = :rawDataId and r.job.id = :jobId",
                            RawData.class)
                    .setParameter("jobId", jobId)
                    .setParameter("rawDataId", rawDataId)
                    .getSingleResult());
        } catch (NoResultException | NonUniqueResultException e) {
            return Optional.empty();
        }
    }

    public void delete(Long jobId) {
        getEntityManager().createQuery("delete from RawData r where job.id = :jobId")
                .setParameter("jobId", jobId)
                .executeUpdate();
        Job job = getEntityManager().getReference(Job.class, jobId);
        getEntityManager().remove(job);
    }

    public List<RawData> findRawDataByJobId(long jobId, int sampling) {
        return getEntityManager().createQuery("select r from RawData r where job.id = :jobId order by r.id desc",
                        RawData.class)
                .setParameter("jobId", jobId)
                .setMaxResults(sampling) // TODO needs to be smarter
                .getResultList();

    }
}
