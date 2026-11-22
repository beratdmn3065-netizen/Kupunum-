# Kuponum oran servisi

Bu servis maç fikstürü ve lisanslı sağlayıcının sunduğu bookmaker oranlarını uygulamaya aktarır. API anahtarı Android uygulamasına konulmaz.

## Kurulum için gerekenler
1. API-Football / API-Sports hesabından bir API anahtarı alın. Oran kapsamı ve istek limitleri seçilen pakete göre değişebilir.
2. Cloudflare hesabında Workers & Pages bölümünden bu klasörü Worker olarak dağıtın.
3. Worker secret olarak `API_FOOTBALL_KEY` ekleyin.
4. Worker adresini Kuponum içindeki **Oran servisi** alanına yazın. Örnek: `https://kuponum-odds-api.<hesap>.workers.dev`

Worker `GET /fixtures?date=YYYY-MM-DD` endpoint'ini sunar. Cevap, maçları ve sağlayıcının o maç için verdiği oranları birlikte döndürür. Kaynakta bulunmayan maç veya oranlar uydurulmaz.

## Önemli
- Bu depo otomatik olarak bir API hesabı veya API anahtarı oluşturamaz.
- API-Football'ın oran kapsamı lig, maç ve bookmaker'a göre değişebilir; her maçta oran bulunması garanti değildir.
- API anahtarı yalnızca Worker secret alanında tutulmalıdır.
- Bu uygulama kupon takip aracıdır; bahis oynatmaz veya ödeme almaz.
