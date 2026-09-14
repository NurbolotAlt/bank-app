package com.github.nurbolotalt.bank_app.Transaction;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "initiated_by", nullable = false)
    private String initiatedBy;

    protected Transaction() {}

    public Transaction(String idempotencyKey, String type, String initiatedBy) {
        this.idempotencyKey = idempotencyKey;
        this.type = type;
        this.initiatedBy = initiatedBy;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getInitiatedBy() { return initiatedBy; }
    public void markCompleted() {
        this.status = "COMPLETED";
    }
}