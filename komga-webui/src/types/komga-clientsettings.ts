export interface ClientSettingDto {
  value: string,
  allowUnauthorized?: boolean,
}

export interface ClientSettingGlobalUpdateDto {
  value: string,
  allowUnauthorized: boolean,
}

export interface ClientSettingUserUpdateDto {
  value: string,
}

export enum CLIENT_SETTING {
  WEBUI_OAUTH2_HIDE_LOGIN = 'webui.oauth2.hide_login',
  WEBUI_OAUTH2_AUTO_LOGIN = 'webui.oauth2.auto_login',
  WEBUI_POSTER_STRETCH = 'webui.poster.stretch',
  WEBUI_POSTER_BLUR_UNREAD = 'webui.poster.blur_unread',
  WEBUI_LIBRARIES = 'webui.libraries',
  WEBUI_SERIES_GROUPS = 'webui.series_groups',
  WEBUI_RECOMMENDED = 'webui.recommended',
}

export interface ClientSettingLibrary {
  unpinned?: boolean,
  order?: number,
}

export interface ClientSettingLibraryUpdate {
  libraryId: string,
  patch: ClientSettingLibrary,
}

export interface ClientSettingsSeriesGroup {
  name: string,
  label?: string,
  groups: Record<string, string[]>
}

export const SERIES_GROUP_ALPHA = {
  name: 'alpha',
  groups: {
    'A': ['A'],
    'B': ['B'],
    'C': ['C'],
    'D': ['D'],
    'E': ['E'],
    'F': ['F'],
    'G': ['G'],
    'H': ['H'],
    'I': ['I'],
    'J': ['J'],
    'K': ['K'],
    'L': ['L'],
    'M': ['M'],
    'N': ['N'],
    'O': ['O'],
    'P': ['P'],
    'Q': ['Q'],
    'R': ['R'],
    'S': ['S'],
    'T': ['T'],
    'U': ['U'],
    'V': ['V'],
    'W': ['W'],
    'X': ['X'],
    'Y': ['Y'],
    'Z': ['Z'],
  },
} as ClientSettingsSeriesGroup

export const SERIES_GROUP_JAPANESE = {
  name: 'japanese',
  label: 'あ〜ん',
  groups: {
    'あ': ['あ', 'ア'],
    'い': ['い', 'イ'],
    'う': ['う', 'ゔ', 'ウ', 'ヴ'],
    'え': ['え', 'エ'],
    'お': ['お', 'オ'],
    'か': ['か', 'が', 'カ', 'ガ'],
    'き': ['き', 'ぎ', 'キ', 'ギ'],
    'く': ['く', 'ぐ', 'ク', 'グ'],
    'け': ['け', 'げ', 'ケ', 'ゲ'],
    'こ': ['こ', 'ご', 'コ', 'ゴ'],
    'さ': ['さ', 'ざ', 'サ', 'ザ'],
    'し': ['し', 'じ', 'シ', 'ジ'],
    'す': ['す', 'ず', 'ス', 'ズ'],
    'せ': ['せ', 'ぜ', 'セ', 'ゼ'],
    'そ': ['そ', 'ぞ', 'ソ', 'ゾ'],
    'た': ['た', 'だ', 'タ', 'ダ'],
    'ち': ['ち', 'ぢ', 'チ', 'ヂ'],
    'つ': ['つ', 'づ', 'ツ', 'ズ'],
    'て': ['て', 'で', 'テ', 'デ'],
    'と': ['と', 'ど', 'ト', 'ド'],
    'な': ['な', 'ナ'],
    'に': ['に', 'ニ'],
    'ぬ': ['ぬ', 'ヌ'],
    'ね': ['ね', 'ネ'],
    'の': ['の', 'ノ'],
    'は': ['は', 'ば', 'ぱ', 'ハ', 'バ', 'パ'],
    'ひ': ['ひ', 'び', 'ぴ', 'ヒ', 'ビ', 'ピ'],
    'ふ': ['ふ', 'ぶ', 'ぷ', 'フ', 'ブ', 'プ'],
    'へ': ['へ', 'べ', 'ぺ', 'ヘ', 'ベ', 'ベ'],
    'ほ': ['ほ', 'ぼ', 'ぽ', 'ホ', 'ボ', 'ポ'],
    'ま': ['ま', 'マ'],
    'み': ['み', 'ミ'],
    'む': ['む', 'ム'],
    'め': ['め', 'メ'],
    'も': ['も', 'モ'],
    'や': ['や', 'ヤ'],
    'ゆ': ['ゆ', 'ユ'],
    'よ': ['よ', 'ヨ'],
    'ら': ['ら', 'ラ'],
    'り': ['り', 'リ'],
    'る': ['る', 'ル'],
    'れ': ['れ', 'レ'],
    'ろ': ['ろ', 'ロ'],
    'わ': ['わ', 'ワ'],
    'を': ['を', 'ヲ'],
    'ん': ['ん', 'ン'],
  },
} as ClientSettingsSeriesGroup

// Groupes cyrilliques (russe). Ё est rattaché à Е comme dans la liste déroulante.
export const SERIES_GROUP_CYRILLIC = {
  name: 'cyrillic',
  label: 'А〜Я',
  groups: {
    'А': ['А'],
    'Б': ['Б'],
    'В': ['В'],
    'Г': ['Г'],
    'Д': ['Д'],
    'Е': ['Е', 'Ё'],
    'Ж': ['Ж'],
    'З': ['З'],
    'И': ['И'],
    'Й': ['Й'],
    'К': ['К'],
    'Л': ['Л'],
    'М': ['М'],
    'Н': ['Н'],
    'О': ['О'],
    'П': ['П'],
    'Р': ['Р'],
    'С': ['С'],
    'Т': ['Т'],
    'У': ['У'],
    'Ф': ['Ф'],
    'Х': ['Х'],
    'Ц': ['Ц'],
    'Ч': ['Ч'],
    'Ш': ['Ш'],
    'Щ': ['Щ'],
    'Ъ': ['Ъ'],
    'Ы': ['Ы'],
    'Ь': ['Ь'],
    'Э': ['Э'],
    'Ю': ['Ю'],
    'Я': ['Я'],
  },
} as ClientSettingsSeriesGroup

// Groupes cyrilliques (ukrainien): Ґ/Є/І/Ї inclus, ъ/ы/э exclus.
export const SERIES_GROUP_CYRILLIC_UK = {
  name: 'cyrillic_uk',
  label: 'А〜Я',
  groups: {
    'А': ['А'],
    'Б': ['Б'],
    'В': ['В'],
    'Г': ['Г', 'Ґ'],
    'Д': ['Д'],
    'Е': ['Е', 'Є'],
    'Ж': ['Ж'],
    'З': ['З'],
    'И': ['И'],
    'І': ['І', 'Ї'],
    'Й': ['Й'],
    'К': ['К'],
    'Л': ['Л'],
    'М': ['М'],
    'Н': ['Н'],
    'О': ['О'],
    'П': ['П'],
    'Р': ['Р'],
    'С': ['С'],
    'Т': ['Т'],
    'У': ['У'],
    'Ф': ['Ф'],
    'Х': ['Х'],
    'Ц': ['Ц'],
    'Ч': ['Ч'],
    'Ш': ['Ш'],
    'Щ': ['Щ'],
    'Ь': ['Ь'],
    'Ю': ['Ю'],
    'Я': ['Я'],
  },
} as ClientSettingsSeriesGroup

// Groupement "local" par langue de l'interface (au-delà de A-Z).
// Les langues non listées n'ont qu'alpha :
// - zh (chinois) : le tri par pinyin correspond déjà à A-Z
// - ko (coréen) : les titres commencent par des syllabes complètes (가...), non filtrables par préfixe ㄱ-ㅎ
// - th/ar/ta : non définis pour le moment (mécanisme prêt, il suffit d'ajouter une entrée ici)
export const SERIES_GROUPS_BY_LOCALE: Record<string, ClientSettingsSeriesGroup> = {
  ja: SERIES_GROUP_JAPANESE,
  ru: SERIES_GROUP_CYRILLIC,
  bg: SERIES_GROUP_CYRILLIC,
  uk: SERIES_GROUP_CYRILLIC_UK,
}

export function getSeriesGroupsForLocale(locale: string): ClientSettingsSeriesGroup | undefined {
  return SERIES_GROUPS_BY_LOCALE[(locale || '').split('-')[0].toLowerCase()]
}

export interface ClientSettingsRecommendedView {
  sections: ClientSettingsRecommendedViewSection[],
}

export interface ClientSettingsRecommendedViewSection {
  section: RecommendedViewSection,
}

export enum RecommendedViewSection {
  KEEP_READING = 'KEEP_READING',
  ON_DECK = 'ON_DECK',
  RECENTLY_RELEASED_BOOKS = 'RECENTLY_RELEASED_BOOKS',
  RECENTLY_ADDED_BOOKS = 'RECENTLY_ADDED_BOOKS',
  RECENTLY_ADDED_SERIES = 'RECENTLY_ADDED_SERIES',
  RECENTLY_UPDATED_SERIES = 'RECENTLY_UPDATED_SERIES',
  RECENTLY_READ_BOOKS = 'RECENTLY_READ_BOOKS',
}

export const RECOMMENDED_DEFAULT = {
  sections: [
    {section: RecommendedViewSection.KEEP_READING},
    {section: RecommendedViewSection.ON_DECK},
    {section: RecommendedViewSection.RECENTLY_RELEASED_BOOKS},
    {section: RecommendedViewSection.RECENTLY_ADDED_BOOKS},
    {section: RecommendedViewSection.RECENTLY_ADDED_SERIES},
    {section: RecommendedViewSection.RECENTLY_UPDATED_SERIES},
    {section: RecommendedViewSection.RECENTLY_READ_BOOKS},
  ],
} as ClientSettingsRecommendedView
