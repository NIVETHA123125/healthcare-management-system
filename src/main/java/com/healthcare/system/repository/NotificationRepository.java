package com.healthcare.system.repository;

import com.healthcare.system.entity.Notification;
import com.healthcare.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE (n.user = :user OR (n.user IS NULL AND n.role = :role)) ORDER BY n.createdAt DESC")
    List<Notification> findByUserOrRoleOrderByCreatedAtDesc(@Param("user") User user, @Param("role") String role);

    @Query("SELECT n FROM Notification n WHERE (n.user = :user OR (n.user IS NULL AND n.role = :role)) AND n.read = :read ORDER BY n.createdAt DESC")
    List<Notification> findByUserOrRoleAndReadOrderByCreatedAtDesc(@Param("user") User user, @Param("role") String role, @Param("read") boolean read);

    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.user = :user OR (n.user IS NULL AND n.role = :role)) AND n.read = :read")
    long countByUserOrRoleAndRead(@Param("user") User user, @Param("role") String role, @Param("read") boolean read);

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Notification n WHERE n.user.id = :userId AND n.type = :type AND n.referenceId = :referenceId")
    boolean existsByUserIdAndTypeAndReferenceId(@Param("userId") Long userId, @Param("type") String type, @Param("referenceId") Long referenceId);
}
