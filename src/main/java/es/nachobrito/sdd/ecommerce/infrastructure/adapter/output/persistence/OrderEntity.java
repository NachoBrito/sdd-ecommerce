package es.nachobrito.sdd.ecommerce.infrastructure.adapter.output.persistence;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

/**
 * Micronaut Data JDBC persistence entity for orders table.
 */
@Serdeable
@MappedEntity("orders")
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.TooManyFields"})
public class OrderEntity {

    @Id
    @MappedProperty("id")
    private UUID id;

    @MappedProperty("customer_name")
    private String customerName;

    @MappedProperty("customer_email")
    private String customerEmail;

    @MappedProperty("date")
    private Instant date;

    @MappedProperty("status")
    private String status;

    @MappedProperty("total_amount_in_cents")
    private Integer totalAmountInCents;

    @MappedProperty("currency")
    private String currency;

    @MappedProperty("delivery_name")
    private String deliveryName;

    @MappedProperty("delivery_telephone")
    private String deliveryTelephone;

    @MappedProperty("delivery_street")
    private String deliveryStreet;

    @Nullable
    @MappedProperty("delivery_complement")
    private String deliveryComplement;

    @MappedProperty("delivery_city")
    private String deliveryCity;

    @MappedProperty("delivery_state")
    private String deliveryState;

    @MappedProperty("delivery_zip_code")
    private String deliveryZipCode;

    @MappedProperty("delivery_country")
    private String deliveryCountry;

    @MappedProperty("invoice_name")
    private String invoiceName;

    @MappedProperty("invoice_vat_number")
    private String invoiceVatNumber;

    @MappedProperty("invoice_telephone")
    private String invoiceTelephone;

    @MappedProperty("invoice_street")
    private String invoiceStreet;

    @Nullable
    @MappedProperty("invoice_complement")
    private String invoiceComplement;

    @MappedProperty("invoice_city")
    private String invoiceCity;

    @MappedProperty("invoice_state")
    private String invoiceState;

    @MappedProperty("invoice_zip_code")
    private String invoiceZipCode;

    @MappedProperty("invoice_country")
    private String invoiceCountry;

    @DateCreated
    @MappedProperty("created_at")
    private Instant createdAt;

    @DateUpdated
    @MappedProperty("updated_at")
    private Instant updatedAt;

    /**
     * Default constructor for serialization frameworks.
     */
    public OrderEntity() {
    }

    /**
     * Full parameter constructor.
     */
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public OrderEntity(
            final UUID id,
            final String customerName,
            final String customerEmail,
            final Instant date,
            final String status,
            final Integer totalAmountInCents,
            final String currency,
            final String deliveryName,
            final String deliveryTelephone,
            final String deliveryStreet,
            final String deliveryComplement,
            final String deliveryCity,
            final String deliveryState,
            final String deliveryZipCode,
            final String deliveryCountry,
            final String invoiceName,
            final String invoiceVatNumber,
            final String invoiceTelephone,
            final String invoiceStreet,
            final String invoiceComplement,
            final String invoiceCity,
            final String invoiceState,
            final String invoiceZipCode,
            final String invoiceCountry,
            final Instant createdAt,
            final Instant updatedAt
    ) {
        this.id = id;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.date = date;
        this.status = status;
        this.totalAmountInCents = totalAmountInCents;
        this.currency = currency;
        this.deliveryName = deliveryName;
        this.deliveryTelephone = deliveryTelephone;
        this.deliveryStreet = deliveryStreet;
        this.deliveryComplement = deliveryComplement;
        this.deliveryCity = deliveryCity;
        this.deliveryState = deliveryState;
        this.deliveryZipCode = deliveryZipCode;
        this.deliveryCountry = deliveryCountry;
        this.invoiceName = invoiceName;
        this.invoiceVatNumber = invoiceVatNumber;
        this.invoiceTelephone = invoiceTelephone;
        this.invoiceStreet = invoiceStreet;
        this.invoiceComplement = invoiceComplement;
        this.invoiceCity = invoiceCity;
        this.invoiceState = invoiceState;
        this.invoiceZipCode = invoiceZipCode;
        this.invoiceCountry = invoiceCountry;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(final Instant date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Integer getTotalAmountInCents() {
        return totalAmountInCents;
    }

    public void setTotalAmountInCents(final Integer totalAmountInCents) {
        this.totalAmountInCents = totalAmountInCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public String getDeliveryName() {
        return deliveryName;
    }

    public void setDeliveryName(final String deliveryName) {
        this.deliveryName = deliveryName;
    }

    public String getDeliveryTelephone() {
        return deliveryTelephone;
    }

    public void setDeliveryTelephone(final String deliveryTelephone) {
        this.deliveryTelephone = deliveryTelephone;
    }

    public String getDeliveryStreet() {
        return deliveryStreet;
    }

    public void setDeliveryStreet(final String deliveryStreet) {
        this.deliveryStreet = deliveryStreet;
    }

    public String getDeliveryComplement() {
        return deliveryComplement;
    }

    public void setDeliveryComplement(final String deliveryComplement) {
        this.deliveryComplement = deliveryComplement;
    }

    public String getDeliveryCity() {
        return deliveryCity;
    }

    public void setDeliveryCity(final String deliveryCity) {
        this.deliveryCity = deliveryCity;
    }

    public String getDeliveryState() {
        return deliveryState;
    }

    public void setDeliveryState(final String deliveryState) {
        this.deliveryState = deliveryState;
    }

    public String getDeliveryZipCode() {
        return deliveryZipCode;
    }

    public void setDeliveryZipCode(final String deliveryZipCode) {
        this.deliveryZipCode = deliveryZipCode;
    }

    public String getDeliveryCountry() {
        return deliveryCountry;
    }

    public void setDeliveryCountry(final String deliveryCountry) {
        this.deliveryCountry = deliveryCountry;
    }

    public String getInvoiceName() {
        return invoiceName;
    }

    public void setInvoiceName(final String invoiceName) {
        this.invoiceName = invoiceName;
    }

    public String getInvoiceVatNumber() {
        return invoiceVatNumber;
    }

    public void setInvoiceVatNumber(final String invoiceVatNumber) {
        this.invoiceVatNumber = invoiceVatNumber;
    }

    public String getInvoiceTelephone() {
        return invoiceTelephone;
    }

    public void setInvoiceTelephone(final String invoiceTelephone) {
        this.invoiceTelephone = invoiceTelephone;
    }

    public String getInvoiceStreet() {
        return invoiceStreet;
    }

    public void setInvoiceStreet(final String invoiceStreet) {
        this.invoiceStreet = invoiceStreet;
    }

    public String getInvoiceComplement() {
        return invoiceComplement;
    }

    public void setInvoiceComplement(final String invoiceComplement) {
        this.invoiceComplement = invoiceComplement;
    }

    public String getInvoiceCity() {
        return invoiceCity;
    }

    public void setInvoiceCity(final String invoiceCity) {
        this.invoiceCity = invoiceCity;
    }

    public String getInvoiceState() {
        return invoiceState;
    }

    public void setInvoiceState(final String invoiceState) {
        this.invoiceState = invoiceState;
    }

    public String getInvoiceZipCode() {
        return invoiceZipCode;
    }

    public void setInvoiceZipCode(final String invoiceZipCode) {
        this.invoiceZipCode = invoiceZipCode;
    }

    public String getInvoiceCountry() {
        return invoiceCountry;
    }

    public void setInvoiceCountry(final String invoiceCountry) {
        this.invoiceCountry = invoiceCountry;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(final String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(final String customerEmail) {
        this.customerEmail = customerEmail;
    }
}
