import type { AxiosInstance } from 'axios';
import type { Billing, BillingPeriod, Checkout, PaymentMethod } from '@/domain/models/Billing';
import type { BillingRepository } from '@/domain/repositories/BillingRepository';
import { toBilling, type BillingDto, type CheckoutDto } from './mappers';

export class HttpBillingRepository implements BillingRepository {
  constructor(private readonly http: AxiosInstance) {}

  async get(): Promise<Billing> {
    const { data } = await this.http.get<BillingDto>('/billing');
    return toBilling(data);
  }

  async startCheckout(method: PaymentMethod, period: BillingPeriod): Promise<Checkout> {
    const { data } = await this.http.post<CheckoutDto>('/billing/checkout', { method, period });
    return { ...data };
  }

  async confirm(reference: string): Promise<Billing> {
    const { data } = await this.http.post<BillingDto>('/billing/confirm', null, { params: { reference } });
    return toBilling(data);
  }

  async cancel(): Promise<Billing> {
    const { data } = await this.http.delete<BillingDto>('/billing');
    return toBilling(data);
  }
}
