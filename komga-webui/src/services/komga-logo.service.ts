import {AxiosInstance} from 'axios'

const API_LOGO = '/api/v1/logo'

export default class KomgaLogoService {
  private http: AxiosInstance

  constructor(http: AxiosInstance) {
    this.http = http
  }

  logoUrl(): string {
    return API_LOGO
  }

  async uploadLogo(file: File): Promise<void> {
    try {
      const formData = new FormData()
      formData.append('file', file)
      await this.http.post(API_LOGO, formData, {
        headers: {'Content-Type': 'multipart/form-data'},
      })
    } catch (e) {
      let msg = 'An error occurred while trying to upload the logo'
      if (e.response?.data?.message) {
        msg += `: ${e.response.data.message}`
      }
      throw new Error(msg)
    }
  }

  async deleteLogo(): Promise<void> {
    try {
      await this.http.delete(API_LOGO)
    } catch (e) {
      let msg = 'An error occurred while trying to delete the logo'
      if (e.response?.data?.message) {
        msg += `: ${e.response.data.message}`
      }
      throw new Error(msg)
    }
  }

  async hasCustomLogo(): Promise<boolean> {
    try {
      await this.http.head(API_LOGO)
      return true
    } catch (e) {
      return false
    }
  }
}
