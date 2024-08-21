package com.example.springplayground.account;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.Nullable;

/**
 * @author d0m1.k0v4c5 on 2024. 08. 19.
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sp_account")
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sp_id_seq")
	@SequenceGenerator(name = "sp_id_seq", sequenceName = "sp_id_seq", allocationSize = 1)
	private Long id;

	@Version
	@Column(nullable = false)
	private Integer version;

	@CreatedDate
	@Column(nullable = false)
	protected LocalDateTime created;

	@Nullable
	@LastModifiedDate
	protected LocalDateTime lastModified;

	@Column(length = 34)
	@Length(max = 34)
	private String accountNumber;

	@Column(length = 64)
	@Length(max = 64)
	private String accountName;

}
