package com.itranswarp.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "wikis")
public class Wiki extends AbstractEntity {

	@Column(nullable = false, length = VAR_ID)
	public String imageId;

	@Column(nullable = false, length = VAR_ID)
	public String textId;

	@Column(nullable = false, length = VAR_ID)
	public String userId;

	@Column(nullable = false, length = VAR_ENUM)
	public String tag;

	@Column(nullable = false, length = VAR_CHAR_NAME)
	public String name;

	@Column(nullable = false, length = VAR_CHAR_DESCRIPTION)
	public String description;

	@Column(nullable = false)
	public long views;

	@Column(nullable = false)
	public long publishAt;

	@Transient
	private List<WikiPage> children = null;

	public void addChild(WikiPage wikiPage) {
		if (this.children == null) {
			this.children = new ArrayList<>();
		}
		this.children.add(wikiPage);
	}

	@Transient
	public List<WikiPage> getChildren() {
		return this.children == null ? List.of() : this.children;
	}

}
