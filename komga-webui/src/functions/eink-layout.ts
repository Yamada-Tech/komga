export interface EinkColumnsInput {
  width: number
  height: number
  compactMode: boolean
  minColumnsPortrait: number
  minColumnsLandscape: number
  maxColumns: number
  minCardWidthCompactPortrait: number
  minCardWidthCompactLandscape: number
  minCardWidthPortrait: number
  minCardWidthLandscape: number
  horizontalPaddingCompact: number
  horizontalPaddingRegular: number
  minAvailableWidth?: number
}

export interface EinkRowsInput {
  width: number
  height: number
  compactMode: boolean
  columns: number
  reservedHeight: number
  minRowsPortrait: number
  minRowsLandscape: number
  maxRows: number
  baseOffset: number
  gridPaddingPortraitCompact: number
  gridPaddingPortraitRegular: number
  gridPaddingLandscape: number
  safetyPaddingPortraitCompact: number
  safetyPaddingPortraitRegular: number
  safetyPaddingLandscape: number
  cardMetaHeightCompact: number
  cardMetaHeightRegular: number
  rowSafetyRatioPortrait: number
  rowSafetyRatioLandscape: number
  itemWidthInsetCompact: number
  itemWidthInsetRegular: number
  horizontalPaddingCompact: number
  horizontalPaddingRegular: number
  minAvailableWidth?: number
  minAvailableHeight?: number
  minItemWidth?: number
}

export interface EinkReservedHeightInput {
  width: number
  height: number
  shortSideThresholds: [number, number]
  portraitValues: [number, number, number]
  landscapeValues: [number, number, number]
}

export function computeEinkCompactMode(width: number, height: number): boolean {
  return Math.min(width, height) <= 430 || (width * height) <= 320000
}

export function computeEinkColumns(input: EinkColumnsInput): number {
  const isPortrait = input.height >= input.width
  const availableWidth = Math.max(
    input.minAvailableWidth ?? 220,
    input.width - (input.compactMode ? input.horizontalPaddingCompact : input.horizontalPaddingRegular),
  )
  const minCardWidth = input.compactMode
    ? (isPortrait ? input.minCardWidthCompactPortrait : input.minCardWidthCompactLandscape)
    : (isPortrait ? input.minCardWidthPortrait : input.minCardWidthLandscape)
  const minColumns = isPortrait ? input.minColumnsPortrait : input.minColumnsLandscape
  return Math.max(minColumns, Math.min(input.maxColumns, Math.floor(availableWidth / minCardWidth)))
}

export function computeEinkRows(input: EinkRowsInput): number {
  const isPortrait = input.height >= input.width
  const availableWidth = Math.max(
    input.minAvailableWidth ?? 220,
    input.width - (input.compactMode ? input.horizontalPaddingCompact : input.horizontalPaddingRegular),
  )
  const gridPadding = isPortrait
    ? (input.compactMode ? input.gridPaddingPortraitCompact : input.gridPaddingPortraitRegular)
    : input.gridPaddingLandscape
  const safetyPadding = isPortrait
    ? (input.compactMode ? input.safetyPaddingPortraitCompact : input.safetyPaddingPortraitRegular)
    : input.safetyPaddingLandscape

  const availableHeight = Math.max(
    input.minAvailableHeight ?? 180,
    input.height - input.reservedHeight - input.baseOffset - gridPadding - safetyPadding,
  )

  const itemWidth = Math.max(
    input.minItemWidth ?? 84,
    Math.floor((availableWidth / input.columns) - (input.compactMode ? input.itemWidthInsetCompact : input.itemWidthInsetRegular)),
  )

  const cardMetaHeight = input.compactMode ? input.cardMetaHeightCompact : input.cardMetaHeightRegular
  const rowSafetyRatio = isPortrait ? input.rowSafetyRatioPortrait : input.rowSafetyRatioLandscape
  const estimatedCardHeight = Math.round(((itemWidth / 0.7071) + cardMetaHeight) * rowSafetyRatio)

  const minRows = isPortrait ? input.minRowsPortrait : input.minRowsLandscape
  return Math.max(minRows, Math.min(input.maxRows, Math.floor(availableHeight / estimatedCardHeight)))
}

export function computeEinkReservedHeight(input: EinkReservedHeightInput): number {
  const isPortrait = input.height >= input.width
  const shortSide = Math.min(input.width, input.height)
  const values = isPortrait ? input.portraitValues : input.landscapeValues

  if (shortSide <= input.shortSideThresholds[0]) return values[0]
  if (shortSide <= input.shortSideThresholds[1]) return values[1]
  return values[2]
}
