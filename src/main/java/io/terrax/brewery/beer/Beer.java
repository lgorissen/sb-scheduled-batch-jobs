package io.terrax.brewery.beer;

public class Beer {
	
	private String id;
	private String name;
	private String releaseDate;
	private Manufacturer manufacturer;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getReleaseDate() {
		return releaseDate;
	}
	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}
	public Manufacturer getManufacturer() {
		return manufacturer;
	}
	public void setManufacturer(Manufacturer manufacturer) {
		this.manufacturer = manufacturer;
	}
	@Override
	public String toString() {
		return "Beer [id=" + id + ", name=" + name + ", releaseDate=" + releaseDate + ", manufacturer=" + manufacturer
				+ "]";
	}
	

}
