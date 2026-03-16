# Feature Research

**Domain:** Energy community network detail page (autoconsommation collective)
**Researched:** 2026-03-16
**Confidence:** HIGH

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Interactive map with network zone | Users need to visualize where the network operates; every energy community platform shows a map with geographic boundaries | MEDIUM | Google Maps already integrated; reuse `draw-network-circles!` pattern from eligibility form. Must show a single network circle centered on network coordinates. |
| Production points on map | Users want to see where the energy comes from; markers for each production site are standard on platforms like Enogrid and Maps Energy | MEDIUM | Requires new `find-by-network-id` on ProductionRepo (does not exist yet). Plot markers from production addresses/coordinates. |
| Network name and location | Basic identity of the network; every detail page starts with a clear name and city/area | LOW | Network entity already has `:network/name`, `:network/center-lat`, `:network/center-lng`. Reverse geocode center to city name, or add a `:network/city` field. |
| Total installed capacity (kWc) | Key metric that communicates network size and credibility; standard on all ACC platforms | LOW | Sum of `:production/installed-power` from active productions linked to the network. Backend aggregation needed. |
| Number of producers | Social proof and network viability signal; users want to know the project is real | LOW | Count of active productions per network. New backend query. |
| Number of consumers (anonymized count only) | Social proof; shows community adoption. Must be anonymous per PROJECT.md | LOW | `count-by-network-id` already exists on ConsumptionRepo. Filter to active/pending only. |
| List of productions (name, energy type, location) | Users need to understand what powers the network; table stakes per PROJECT.md requirements | MEDIUM | Requires productions to have displayable names (producer name or installation name). Currently productions have `:production/producer-address` but no display name. May need to join with user data or add a field. |
| Energy mix breakdown (% by type) | Users want to know if it is solar, wind, hydro etc.; differentiates from generic green energy claims | LOW | Compute from `:production/energy-type` across network productions. Simple percentage calculation. |
| Call-to-action: join/sign up | Without a clear CTA the page is informational dead-end; must convert interest into action | LOW | Link to eligibility check or signup page. Already exists in eligibility form (`rfee/href :page/signup`). |
| Descriptive text about the network | Context about what ACC is and why to join; sets expectations for non-expert visitors | LOW | Generated content per PROJECT.md decision. Template: "Rejoignez l'Operation d'Autoconsommation Collective a [ville]". No CMS needed. |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valuable.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Estimated savings indicator | Converts abstract kWc into tangible value ("economisez ~X EUR/an"); motivates sign-up far more than raw capacity numbers | MEDIUM | Requires assumptions about local tariffs vs. ACC price. Can use `consumption/price-per-kwh` as basis. Even a rough range ("10-15% d'economie") is powerful. |
| Environmental impact summary | "Equivalent to X tonnes CO2 avoided" or "powers X households"; makes the network's value concrete and shareable | LOW | Standard conversion factors (1 kWc solar ~ 1100 kWh/year in France, ~50g CO2/kWh avoided vs. grid). Static calculation from installed capacity. |
| Responsive mobile-first layout | Many users discover the page from a mobile eligibility check; poor mobile map experience kills conversion | MEDIUM | Google Maps is inherently responsive but page layout, stat cards, and production list need mobile attention. |
| Share/social proof buttons | Users who just discovered eligibility want to tell neighbors; viral loop for community growth | LOW | Simple share links (URL copy, WhatsApp, email). No API integration needed. |
| Animated statistics (count-up) | Visual polish that makes the page feel professional and alive, improving trust | LOW | CSS/JS count-up animation on key numbers (kWc, producers, consumers). Small frontend effort. |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Real-time production data | "Show live solar output" sounds impressive | Requires Enedis API integration, data pipeline, storage for time series, and ongoing maintenance. Massive scope for a detail page. OUT OF SCOPE per PROJECT.md. | Show installed capacity as a static metric. Add real-time data in a future milestone when the exploitation phase (like EnoPower) is built. |
| Consumer list or details | "Show who else joined" for social proof | Privacy violation; GDPR concerns; consumers did not consent to public display. Explicitly OUT OF SCOPE per PROJECT.md. | Show anonymous consumer count only. |
| Network listing/discovery page | "Let users browse all available networks" | Exposes network geography before eligibility check; reduces the conversion funnel's effectiveness; premature for MVP with few networks. OUT OF SCOPE per PROJECT.md. | Access via eligibility check only. Eligible users see their network's detail page. |
| CMS/admin-managed content | "Let admins customize network descriptions" | Adds back-office complexity, rich text editor, content moderation. Overkill when networks are few and content can be templated. | Generate descriptions from network data (name, city, capacity, energy types). Revisit CMS when there are 20+ networks with distinct stories. |
| Historical production charts | "Show monthly/yearly production graphs" | Requires time-series data collection infrastructure that does not exist yet. | Defer to exploitation phase. A static "annual estimated production" number suffices for the detail page. |
| Interactive eligibility re-check on detail page | "Let visitors re-verify eligibility from the network page" | Duplicates the home page eligibility form; confusing UX with two entry points doing the same thing. | The detail page is reached AFTER eligibility check. Show a "Vous etes eligible" confirmation banner instead, with CTA to sign up. |

## Feature Dependencies

```
[Network API endpoint (public, by ID)]
    +-- requires --> [Network entity exists] (DONE)
    +-- requires --> [Productions linked to network queryable]
                         +-- requires --> [ProductionRepo.find-by-network-id] (NEW)

[Map with production markers]
    +-- requires --> [Network API endpoint]
    +-- requires --> [Production coordinates/addresses]
    +-- requires --> [Google Maps component] (DONE)

[Statistics (capacity, mix, counts)]
    +-- requires --> [Productions by network query]
    +-- requires --> [Consumption count by network] (DONE)

[Generated description text]
    +-- requires --> [Network name + city]
    +-- requires --> [Statistics computed]

[CTA button]
    +-- requires --> [Signup/eligibility page] (DONE)
    +-- enhances --> [Generated description text]

[Estimated savings]
    +-- requires --> [Statistics (capacity)]
    +-- enhances --> [CTA button]

[Environmental impact]
    +-- requires --> [Statistics (capacity)]
    +-- enhances --> [Generated description text]
```

### Dependency Notes

- **Map with markers requires ProductionRepo.find-by-network-id:** The Production protocol currently has `find-all` and `find-by-user-id` but no way to query by network. A new protocol method and XTDB query are needed.
- **Statistics require productions by network:** Capacity sum, energy mix, and producer count all derive from the same production list.
- **Generated description requires statistics:** The template text ("X kWc de puissance installee, Y producteurs...") needs computed values.
- **Estimated savings enhances CTA:** Showing savings next to the sign-up button increases conversion. Build savings after core stats are working.

## MVP Definition

### Launch With (v1)

Minimum viable product -- what is needed to validate the concept.

- [x] Public API endpoint: `GET /api/v1/networks/:id/detail` returning network + aggregated stats + productions list -- the data backbone for the page
- [x] `ProductionRepo.find-by-network-id` -- new protocol method filtering active productions by `:production/network-id`
- [x] Interactive Google Map showing network zone circle + production site markers -- the visual anchor
- [x] Statistics panel: total kWc, number of producers, number of consumers, energy mix percentages -- the credibility layer
- [x] Production list: name/label, energy type, location -- transparency about energy sources
- [x] Generated description text from template -- contextual narrative
- [x] CTA button linking to signup -- the conversion mechanism
- [x] Frontend route: `/reseau/:id` accessible after eligibility check result

### Add After Validation (v1.x)

Features to add once core is working.

- [ ] Estimated savings indicator -- once we have validated tariff assumptions
- [ ] Environmental impact summary (CO2 avoided, households equivalent) -- once capacity data is confirmed accurate
- [ ] Share buttons (copy link, WhatsApp, email) -- once page URL structure is stable
- [ ] Animated count-up on statistics -- polish after core UX is validated
- [ ] Mobile-optimized layout -- test with real users on mobile first, then optimize

### Future Consideration (v2+)

Features to defer until product-market fit is established.

- [ ] Real-time production data integration (Enedis API) -- requires separate infrastructure milestone
- [ ] CMS-managed content per network -- when network count exceeds template usefulness
- [ ] Network comparison page -- when multiple networks exist in same area
- [ ] Historical production/consumption charts -- requires time-series data pipeline
- [ ] Testimonials from existing consumers -- requires content collection workflow

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Public network detail API endpoint | HIGH | MEDIUM | P1 |
| ProductionRepo.find-by-network-id | HIGH | LOW | P1 |
| Map with zone circle + markers | HIGH | MEDIUM | P1 |
| Statistics panel (kWc, counts, mix) | HIGH | LOW | P1 |
| Production list | HIGH | MEDIUM | P1 |
| Generated description text | MEDIUM | LOW | P1 |
| CTA button to signup | HIGH | LOW | P1 |
| Frontend route `/reseau/:id` | HIGH | MEDIUM | P1 |
| Estimated savings indicator | HIGH | MEDIUM | P2 |
| Environmental impact summary | MEDIUM | LOW | P2 |
| Share buttons | MEDIUM | LOW | P2 |
| Count-up animations | LOW | LOW | P3 |
| Mobile layout optimization | MEDIUM | MEDIUM | P2 |

**Priority key:**
- P1: Must have for launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration

## Competitor Feature Analysis

| Feature | Enogrid (Mon energie collective) | Maps Energy (ROSE Designer) | ProxyWatt (our approach) |
|---------|----------------------------------|----------------------------|--------------------------|
| Network map | Map with participant locations and zone | Interactive map with energy community boundaries | Google Maps with network circle + production markers |
| Production details | Producer list with contract status | Detailed technical specs per installation | Public list: name, energy type, location (no contract details) |
| Statistics | Capacity, participant count, load curves | KPIs: energy shared, self-consumption rate, economic indicators | Capacity (kWc), producer count, consumer count, energy mix |
| Real-time data | Via EnoPower (exploitation phase) | Simulation-based, not real-time | Not in scope -- static metrics only |
| CTA / recruitment | Built-in consumer recruitment flow | N/A (B2B tool) | Single CTA to signup after eligibility check |
| Content | Admin-configurable project descriptions | Auto-generated technical reports | Template-generated descriptions from network data |
| Savings estimate | Economic analysis via EnoLab | Full financial simulation | Simple estimate based on tariff differential (P2) |

## Sources

- [Enogrid tools overview](https://enogrid.com/outils/) -- MEDIUM confidence, fetched 2026-03-16
- [Enogrid delegation and ACC management](https://enogrid.com/delegation/) -- MEDIUM confidence
- [Mon energie collective FAQ](https://monenergiecollective.fr/faq/articles/en-devenant-producteur-dans-une-autoconsommation-collective-quelles-sont-mes-obligations) -- MEDIUM confidence
- [Maps Energy Community Designer](https://energy.mapsgroup.it/en/energy-community-designer-en/) -- MEDIUM confidence
- [Energy Community Platform EU](https://energycommunityplatform.eu/) -- MEDIUM confidence
- [Enedis autoconsommation observatory](https://observatoire.enedis.fr/autoconsommation) -- HIGH confidence, official source
- [Reonic ACC guide 2026](https://reonic.com/fr-fr/blog/autoconsommation-collective-guide-complet-2026/) -- MEDIUM confidence
- Existing codebase: `domain/network.clj`, `domain/production.clj`, `domain/consumption.clj`, `components/eligibility_form.cljs`, `components/google_map.cljs` -- HIGH confidence, primary source

---
*Feature research for: Energy community network detail page (autoconsommation collective)*
*Researched: 2026-03-16*
