interface CollectionDto {
  id: string,
  name: string,
  ordered: boolean,
  public: boolean,
  filtered: boolean,
  seriesIds: string[],
  createdDate: Date,
  lastModifiedDate: Date
}

interface CollectionCreationDto {
  name: string,
  ordered: boolean,
  public?: boolean,
  seriesIds: string[]
}

interface CollectionUpdateDto {
  name?: string,
  ordered?: boolean,
  public?: boolean,
  seriesIds?: string[]
}

interface CollectionThumbnailDto {
  id: string,
  collectionId: string,
  type: string,
  selected: boolean,
  mediaType: string,
  fileSize: number,
  width: number,
  height: number,
}
