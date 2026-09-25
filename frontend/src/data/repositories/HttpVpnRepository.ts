import type { AxiosInstance } from 'axios';
import type { VpnAccess, VpnProfileDownload, VpnProtocol } from '@/domain/models/Vpn';
import type { VpnRepository } from '@/domain/repositories/VpnRepository';

interface VpnAccessDto {
  enabled: boolean;
  issuedAt: string | null;
  endpoints: { protocol: string; host: string; port: number }[];
  labNetwork: string;
}

function toVpnAccess(dto: VpnAccessDto): VpnAccess {
  return {
    enabled: dto.enabled,
    issuedAt: dto.issuedAt ? new Date(dto.issuedAt) : null,
    endpoints: dto.endpoints
      .filter((e) => e.protocol === 'udp' || e.protocol === 'tcp')
      .map((e) => ({ protocol: e.protocol as VpnProtocol, host: e.host, port: e.port })),
    labNetwork: dto.labNetwork,
  };
}

/** Extrait le nom de fichier de l'en-tête Content-Disposition. */
function fileNameFrom(header: unknown, protocol: VpnProtocol): string {
  const match = typeof header === 'string' ? /filename="?([^";]+)"?/i.exec(header) : null;
  return match?.[1] ?? `cyberMans-lab-${protocol}.ovpn`;
}

export class HttpVpnRepository implements VpnRepository {
  constructor(private readonly http: AxiosInstance) { }

  async getAccess(): Promise<VpnAccess> {
    const { data } = await this.http.get<VpnAccessDto>('/vpn');
    return toVpnAccess(data);
  }

  async downloadProfile(protocol: VpnProtocol): Promise<VpnProfileDownload> {
    const response = await this.http.get<string>('/vpn/profile', {
      params: { protocol },
      responseType: 'text',
      // Le corps n'est pas du JSON : on le garde tel quel.
      transformResponse: (body: unknown) => body,
    });
    return {
      fileName: fileNameFrom(response.headers['content-disposition'], protocol),
      content: response.data,
    };
  }

  async regenerate(): Promise<VpnAccess> {
    const { data } = await this.http.post<VpnAccessDto>('/vpn/profile/regenerate');
    return toVpnAccess(data);
  }
}
