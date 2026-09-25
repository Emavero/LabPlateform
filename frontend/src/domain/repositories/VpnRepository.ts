import type { VpnAccess, VpnProfileDownload, VpnProtocol } from '../models/Vpn';

export interface VpnRepository {
  getAccess(): Promise<VpnAccess>;
  downloadProfile(protocol: VpnProtocol): Promise<VpnProfileDownload>;
  regenerate(): Promise<VpnAccess>;
}
