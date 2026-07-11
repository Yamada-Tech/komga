interface CollectionDto {
  id: string,
  name: string,
  ordered: boolean,
  showInSidebar: boolean,
  filtered: boolean,
  seriesIds: string[],
  createdDate: Date,
  lastModifiedDate: Date
}

interface CollectionCreationDto {
  name: string,
  ordered: boolean,
  showInSidebar?: boolean,
  seriesIds: string[]
}

interface CollectionUpdateDto {
  name?: string,
  ordered?: boolean,
  showInSidebar?: boolean,
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
