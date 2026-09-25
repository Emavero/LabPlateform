import type { Role, User, UserProfile } from '@/domain/models/User';
import type {
  AccessProtocol,
  OperatingSystem,
  VirtualMachine,
  VmStatus,
} from '@/domain/models/VirtualMachine';

/** Formes JSON exactes renvoyées par l'API. Elles ne sortent pas de la couche data. */
export interface UserDto {
  id: number;
  email: string;
  role: Role;
}

export interface ProfileDto extends UserDto {
  createdAt: string;
}

export interface VmDto {
  id: number;
  os: OperatingSystem;
  osName: string;
  status: VmStatus;
  startedAt: string | null;
  connection: {
    host: string;
    port: number;
    protocol: AccessProtocol;
    username: string;
    password: string;
  } | null;
}

export function toUser(dto: UserDto): User {
  return { id: dto.id, email: dto.email, role: dto.role };
}

export function toUserProfile(dto: ProfileDto): UserProfile {
  return { ...toUser(dto), createdAt: new Date(dto.createdAt) };
}

export function toVirtualMachine(dto: VmDto): VirtualMachine {
  return {
    id: dto.id,
    os: dto.os,
    osName: dto.osName,
    status: dto.status,
    startedAt: dto.startedAt ? new Date(dto.startedAt) : null,
    connection: dto.connection ? { ...dto.connection } : null,
  };
}
