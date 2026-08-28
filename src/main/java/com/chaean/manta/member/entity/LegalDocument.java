package com.chaean.manta.member.entity;

import com.chaean.manta.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "legal_document", schema = "orca")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class LegalDocument extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false, length = 50)
	private LegalDocumentType documentType;

	@Column(nullable = false, columnDefinition = "text")
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "is_required", nullable = false)
	private boolean required;
}
